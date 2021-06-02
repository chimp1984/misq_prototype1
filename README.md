# Misq

[![Release](https://jitpack.io/v/chimp1984/misq.svg)](https://jitpack.io/#chimp1984/misq)

[![Build Status](https://travis-ci.org/github/chimp1984/misq.svg?branch=master)](https://travis-ci.org/github/chimp1984/misq)


## What is Misq?

Work in progress for a new version for Bisq


## Dev notes:

### Dependencies
If new dependencies are added, or a dependency version is changed, use `./gradlew --write-verification-metadata sha256 help` to optimistically bootstrap the list of hashes.

For details, see https://docs.gradle.org/current/userguide/dependency_verification.html

For details on bootstrapping the initial hashes, see https://docs.gradle.org/current/userguide/dependency_verification.html#sec:bootstrapping-verification

### Testing
To ignore tests use the phrase `Integration` in the test class name (e.g `TorIntegrationTest`) and add
```
 test {
        exclude '**/**Integration*'
    }
```
to the module config. This is useful for tests which would require a custom setup (e.g. require I2P installation) and/or take a long time for running (e.g. starting tor/i2p)

### Importing as dependency
If building on top of the Misq codebase, Misq and its modules can be added as a project dependency. See https://jitpack.io/#chimp1984/misq
