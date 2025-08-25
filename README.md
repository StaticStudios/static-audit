# static-audit
<p>
  <a href="https://repo.staticstudios.net/#/snapshots/net/staticstudios/static-audit">
    <img src="https://repo.staticstudios.net/api/badge/latest/snapshots/net/staticstudios/static-audit?color=9ec3ff&name=Maven">
  </a>
  <a href="https://github.com/StaticStudios/Static-Audit">
    <img src="https://img.shields.io/github/actions/workflow/status/StaticStudios/static-audit/publish.yml?branch=master&logo=github">
  </a>
</p>
A simple library for logging and retrieving user actions.

## Installation

```
repositories {
    maven {
        url = "https://repo.staticstudios.net/snapshots/"
    }
}

dependencies {
    implementation("net.staticstudios:static-audit:{VERSION}")
}
```

## Usage
Create an instance of `StaticAudit` using the builder pattern:
```java
StaticAudit audit = StaticAudit.builder()
        .applicationGroup("application_group")
        .applicationId("application_id")
        .connectionSupplier(...)
        .build();
// Log  an action 
audit.log(...);

//Retrieve actions
audit.retrieve(...);
```