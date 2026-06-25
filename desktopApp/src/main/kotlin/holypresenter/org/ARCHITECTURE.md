# HolyPresenter Architecture

HolyPresenter is not only a presentation application.  
It is planned as a modular platform for church worship services.

## Core Principles

1. The core does not depend on concrete modules.
2. Modules depend only on the public platform API.
3. Modules communicate through EventBus, CommandBus and services.
4. UI panels are registered through DockPanel API.
5. A module may be enabled, disabled, loaded and unloaded independently.
6. Projector is a platform module, not a part of timer, songs or bible.
7. Future external modules should be possible without changing the core.

## Main Layers

```text
HolyPresenter
├── app
├── platform
│   ├── api
│   ├── core
│   ├── services
│   ├── commands
│   ├── events
│   ├── docking
│   └── plugins
└── modules
    ├── projector
    ├── timer
    ├── songs
    ├── bible
    ├── media
    └── announcements

```


## Current Foundation

1. ModuleRegistry
2. ModuleContext
3. HolyModule
4. DockManager
5. DockPanel
6. DockContent
7. EventBus
8. CommandBus
9. ServiceRegistry
10. ProjectorModule