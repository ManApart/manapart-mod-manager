# Manapart Mod Manager

[![Build and Test](https://github.com/ManApart/starfield-mod-manager/actions/workflows/runTests.yml/badge.svg)](https://github.com/ManApart/starfield-mod-manager/actions/workflows/runTests.yml)

Flexible CLI commands support everything from Nexus download links to bulk updating mods, to maintaining and deploying local mod collections.

![Demo](demo.svg)

View more info and demos at the docs site:

### [Features](https://manapart.github.io/starfield-mod-manager-site/features.html)

### [Setup](https://manapart.github.io/starfield-mod-manager-site/setup.html)

### [Manual](https://manapart.github.io/starfield-mod-manager-site/manual.html)

### Additional Config

Config files and mod staging used to be placed in the directory of the jar, but has been now moved to the home directory.

Specifically, `HOME` is set to env `MMM_HOME` if it exists, else the `user.home` system property and finally defaults to current directory.

The config and data paths first look for env `XDG_CONFIG_HOME` if it exists, else uses the `HOME` directory above, and then creates a `mmm` folder inside it.
