autwit-project/
│   pom.xml  <-- ROOT AGGREGATOR POM
│
├── autwit-parent/
│       pom.xml
├── autwit-core/
│       pom.xml
├── autwit-engine/
│       pom.xml
├── autwit-adapter-kafka/
│       pom.xml
├── autwit-adapter-mongo/
│       pom.xml
├── autwit-adapter-h2/
│       pom.xml
├── autwit-adapter-postgres/
│       pom.xml
├── autwit-client-sdk/
│       pom.xml
├── autwit-internal-testkit/
│       pom.xml
├── autwit-runner/
│       pom.xml
└── autwit-shared/
        pom.xml

 ---------------------------------------------

 autwit-core
      ↑
 autwit-engine
      ↑
 autwit-adapters (mongo/h2/postgres/kafka)
      ↑
 autwit-testkit
      ↑
 autwit-client-sdk
      ↑
 autwit-runner
-----------------------------------------------

autwit-runner
    ↓ depends on
autwit-client-sdk
    ↓ depends on
autwit-testkit
    ↓ depends on
autwit-adapters (mongo / h2 / postgres / kafka)
    ↓ depends on
autwit-engine
    ↓ depends on
autwit-core

----------------------------------------------

| Module       | Can Depend On                   | Cannot Depend On         |
| ------------ | ------------------------------- | ------------------------ |
| **core**     | Nobody                          | ANYBODY                  |
| **engine**   | core                            | testkit, sdk, runner     |
| **adapters** | engine, core                    | testkit, sdk, runner     |
| **testkit**  | adapters, engine, core          | sdk, runner              |
| **sdk**      | testkit, adapters, engine, core | runner                   |
| **runner**   | everything                      | Nobody depends on runner |

