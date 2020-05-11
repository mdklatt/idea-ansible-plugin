###################
Ansible IDEA Plugin
###################

|travis|

Run `Ansible`_ commands within a `JetBrains`_ IDE like IntelliJ or PyCharm.


`Ansible logo`_ |copy| RedHat, Inc.


===============
Plugin Features
===============

Run Configurations
==================

- Execute Ansible playbooks.
- Install dependencies using `Galaxy`_ (TODO).


=====
Usage
=====

A new category of ``Ansible`` run configurations will be installed under the
IDE *Run->Edit Configurations...* menu.

Run Playbook
============
- ``Playbook``: One or more playbooks to execute (**required**)
- ``Inventory``: One or more inventory files
- ``Host``: Host name/group
- ``Tags``: One or more comma-separated tags
- ``Sudo password``: Sudo password for the target host(s)
- ``Working directory``: Working directory to use


============
Installation
============

Use *Preferences->Plugins->Install Plugin from Disk...* to install a local
copy of the plugin JAR file.


.. _travis: https://travis-ci.org/mdklatt/idea-ansible-plugin
.. _Ansible: https://docs.ansible.com/ansible/latest/index.html
.. _Ansible logo: https://www.ansible.com/logos
.. _Galaxy: https://galaxy.ansible.com
.. _JetBrains: https://www.jetbrains.com

.. |copy| unicode:: U+000A9 .. COPYRIGHT SIGN
.. |travis| image:: https://travis-ci.org/mdklatt/idea-ansible-plugin.png
   :alt: Travis CI build status
   :target: `travis`_
