package misq.finance.contract.sharedState;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import lombok.Getter;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;

import static misq.finance.contract.sharedState.AccessCondition.atom;

// TODO: Need to decide rules concerning mutual incompatibility of @Supplied, @DependsOn, @Access, @Action and @Event annotations.
@Getter
public class SharedStateFactory<T> {
    private final Class<T> clazz;
    private final Set<String> parties;
    private final Set<String> propertyNames;
    private final Multimap<String, Set<String>> dependencyMultimap;
    private final Map<String, String> actorMap;
    private final Map<String, String> supplierMap;
    private final Map<String, String> eventObserverMap;
    private final Map<String, AccessCondition> declaredAccessConditionMap;
    private final Map<String, AccessCondition.DPF> accessConditionMap;

    public SharedStateFactory(Class<T> clazz) {
        Preconditions.checkArgument(clazz.isInterface(), "Not an interface: %s", clazz);
        this.clazz = clazz;
        parties = processSharedStateAnnotation();
        propertyNames = Arrays.stream(clazz.getMethods())
                .filter(m -> m.getParameterCount() == 0)
                .map(Method::getName)
                .collect(ImmutableSet.toImmutableSet());
        dependencyMultimap = processDependsOnAnnotations();
        actorMap = processActionAnnotations();
        supplierMap = processSuppliedAnnotations();
        eventObserverMap = processEventAnnotations();
        declaredAccessConditionMap = processAccessAnnotations();
        accessConditionMap = computeDerivedAccessConditions();
    }

    private Set<String> processSharedStateAnnotation() {
        State annotation = clazz.getAnnotation(State.class);
        Preconditions.checkNotNull(annotation, "Missing @SharedState annotation on %s", clazz);
        Preconditions.checkArgument(annotation.parties().length != 0, "Empty party list on %s", clazz);
        for (String party : annotation.parties()) {
            Preconditions.checkArgument(Character.isJavaIdentifierStart(party.charAt(0)) &&
                            party.chars().allMatch(Character::isJavaIdentifierPart),
                    "Expected: valid Java identifier for party - got: %s", party);
        }
        return ImmutableSet.copyOf(annotation.parties());
    }

    private Multimap<String, Set<String>> processDependsOnAnnotations() {
        var multimapBuilder = ImmutableListMultimap.<String, Set<String>>builder();

        for (Method method : clazz.getMethods()) {
            var dependsOnAnnotations = method.getAnnotationsByType(DependsOn.class);
            var name = method.getName();
            Preconditions.checkArgument(dependsOnAnnotations.length == 0 || propertyNames.contains(name),
                    "Illegal @DependsOn annotation on non-property %s", method);

            for (DependsOn annotation : dependsOnAnnotations) {
                Preconditions.checkArgument(annotation.value().length != 0,
                        "Empty dependency list on property %s", name);
                for (String dependencyName : annotation.value()) {
                    Preconditions.checkArgument(propertyNames.contains(dependencyName),
                            "Unrecognized dependency %s of property %s", dependencyName, name);
                }
                multimapBuilder.put(name, ImmutableSet.copyOf(annotation.value()));
            }
        }
        return multimapBuilder.build();
    }

    // TODO: Should we require void return type for actions?
    private Map<String, String> processActionAnnotations() {
        var mapBuilder = ImmutableMap.<String, String>builder();

        for (Method method : clazz.getMethods()) {
            var actionAnnotation = method.getAnnotation(Action.class);
            var name = method.getName();

            if (actionAnnotation != null) {
                Preconditions.checkArgument(propertyNames.contains(name),
                        "Illegal @Action annotation on non-property %s", method);
                Preconditions.checkArgument(parties.contains(actionAnnotation.by()),
                        "Unrecognized actor %s for action %s", actionAnnotation.by(), name);
                mapBuilder.put(name, actionAnnotation.by());
            }
        }
        return mapBuilder.build();
    }

    private Map<String, String> processSuppliedAnnotations() {
        var mapBuilder = ImmutableMap.<String, String>builder();

        for (Method method : clazz.getMethods()) {
            var suppliedAnnotation = method.getAnnotation(Supplied.class);
            var name = method.getName();

            if (suppliedAnnotation != null) {
                Preconditions.checkArgument(propertyNames.contains(name),
                        "Illegal @Supplied annotation on non-property %s", method);
                Preconditions.checkArgument(parties.contains(suppliedAnnotation.by()),
                        "Unrecognized supplier %s for property %s", suppliedAnnotation.by(), name);
                mapBuilder.put(name, suppliedAnnotation.by());
            }
        }
        return mapBuilder.build();
    }

    private Map<String, String> processEventAnnotations() {
        var mapBuilder = ImmutableMap.<String, String>builder();

        for (Method method : clazz.getMethods()) {
            var eventAnnotation = method.getAnnotation(Event.class);
            var name = method.getName();

            if (eventAnnotation != null) {
                Preconditions.checkArgument(propertyNames.contains(name),
                        "Illegal @Event annotation on non-property %s", method);
                Preconditions.checkArgument(!dependencyMultimap.containsKey(name),
                        "Illegal simultaneous annotation of property %s with @Event and @DependsOn", method);
                Preconditions.checkArgument(parties.contains(eventAnnotation.seenBy()),
                        "Unrecognized observer %s for event %s", eventAnnotation.seenBy(), name);
                mapBuilder.put(name, eventAnnotation.seenBy());
            }
        }
        return mapBuilder.build();
    }

    private Map<String, AccessCondition> processAccessAnnotations() {
        var mapBuilder = ImmutableMap.<String, AccessCondition>builder();
        var allowedNames = Sets.union(parties, eventObserverMap.keySet());

        for (Method method : clazz.getMethods()) {
            var accessAnnotation = method.getAnnotation(Access.class);
            var name = method.getName();

            if (accessAnnotation != null) {
                Preconditions.checkArgument(propertyNames.contains(name),
                        "Illegal @Access annotation on non-property %s", method);
                mapBuilder.put(name, AccessCondition.parse(accessAnnotation.value(), allowedNames));
            }
        }
        return mapBuilder.build();
    }

    private Map<String, AccessCondition.DPF> computeDerivedAccessConditions() {
        var accessConditionMap = new HashMap<>(Maps.asMap(propertyNames, Functions.constant(AccessCondition.none())));
        declaredAccessConditionMap.forEach((id, cond) -> accessConditionMap.merge(id, cond.toDPF(), AccessCondition.DPF::or));
        supplierMap.forEach((id, party) -> accessConditionMap.merge(id, atom(party), AccessCondition.DPF::or));
        eventObserverMap.forEach((id, party) -> accessConditionMap.merge(id, atom(id).and(atom(party)), AccessCondition.DPF::or));

        var dirtyProperties = new HashSet<>(propertyNames);
        var firstRound = true;
        while (true) {
            for (String id : propertyNames) {
                if (dirtyProperties.isEmpty()) {
                    return ImmutableMap.copyOf(accessConditionMap);
                }
                if (!firstRound) {
                    dirtyProperties.remove(id);
                }
                var oldCond = accessConditionMap.get(id);
                for (var dependencies : dependencyMultimap.get(id)) {
                    if (dependencies.stream().anyMatch(dirtyProperties::contains)) {
                        var inheritedCond = dependencies.stream()
                                .map(accessConditionMap::get)
                                .reduce(AccessCondition.DPF::and).orElseThrow();

                        accessConditionMap.merge(id, inheritedCond, AccessCondition.DPF::or);
                    }
                }
                if (!oldCond.equals(accessConditionMap.get(id))) {
                    dirtyProperties.add(id);
                }
            }
            firstRound = false;
        }
    }

    @SafeVarargs
    public static <T> T oneOf(Supplier<T>... alternativeDerivations) {
        throw new UnsupportedOperationException();
    }
}
