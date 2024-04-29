# Ansible Plugin for IDEA

[![IDEA version][1]][7]
[![Latest release][2]][3]
[![Test status][4]][5]

[1]: https://img.shields.io/static/v1?label=IDEA&message=2023.1%2B&color=informational
[2]: https://img.shields.io/github/v/release/mdklatt/idea-ansible-plugin?sort=semver
[3]: https://github.com/mdklatt/idea-ansible-plugin/releases
[4]: https://github.com/mdklatt/idea-ansible-plugin/actions/workflows/test.yml/badge.svg
[5]: https://github.com/mdklatt/idea-ansible-plugin/actions/workflows/test.yml


<!-- This content is used by the Gradle IntelliJ Plugin. --> 
<!-- Plugin description -->

## Introduction

This plugin provides tools for working with [Ansible][8] inside a JetBrains 
IDE. An existing Ansible installation is required.

[Ansible logos][10] &copy; RedHat, Inc.


## Run/Debug Configurations

[Run/debug configurations][6] are provided for executing Ansible commands.
A new configuration can be created from the `Run > Edit Configurations` menu
or by right clicking on a file and using the `Ansible` context menu. 


### Galaxy

Use this configuration to install collections and roles using 
[ansible-galaxy][11]. Use a YAML requirements file to create a new 
*Galaxy* configuration from the context menu.


### Playbook

Use this configuration to execute plays using [ansible-playbook][12]. Use a
YAML playbook to create a new *Playbook* configuration from the context menu.



## Plugin Setup

Per-project settings are found under `Settings > Tools > Ansible`. These are
used to specify where the Ansible executables are installed. This can be a
regular system install or a Python virtualenv. Optionally, a [Docker][15] image
can be used for either installation types (requires [Docker Engine][16] on the 
host machine). An *ansible.cfg* file can also be specified in these settings. 

For Docker execution, the working directory specified in the run configuration
settings becomes the working directory of the Ansible container. Any input 
files that are not part of the image (playbooks, requirements files, 
*ansible.cfg*, *etc.*) should within this directory. Paths on the host must be 
relative to the working directory. Use absolute paths to specify files that are 
part of the image.


[6]: https://www.jetbrains.com/help/idea/run-debug-configuration.htmlhttps://www.jetbrains.com/help/idea/run-debug-configuration.html
[7]: https://www.jetbrains.com
[8]: https://docs.ansible.com/ansible/latest/index.html
[9]: https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html
[10]: https://www.ansible.com/logos
[11]: https://docs.ansible.com/ansible/latest/galaxy/user_guide.html
[12]: https://docs.ansible.com/ansible/latest/cli/ansible-playbook.html
[15]: https://docs.docker.com
[16]: https://docs.docker.com/engine

<!-- Plugin description end -->


## Installation

The latest version is available via a [custom plugin repository][13]. [Releases][3] 
include a binary distribution named `idea-ansible-plugin-<version>.zip` that
can be [installed from disk][14].


[13]: https://mdklatt.github.io/idea-plugin-repo
[14]: https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_disk
