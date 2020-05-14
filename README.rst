###################
Ansible IDEA Plugin
###################

|travis|

Run `Ansible`_ commands within a `JetBrains`_ IDE like IntelliJ or PyCharm.


`Ansible logo`_ |copy| RedHat, Inc.


=====
Usage
=====

Run Configurations
==================

A new category of ``Ansible`` run configurations will be installed under the
*Run->Edit Configurations...* menu.


Playbook
--------

Execute a sequence of playbooks using ``ansible-playbook``.

- ``Playbook``: One or more playbooks to execute (**required**)
- ``Inventory``: One or more inventory files
- ``Host``: Host name/group
- ``Tags``: Spaced-delimited tags
- ``Extra variables``: Space-delimited Ansible variables *e.g.* ``name=value``
- ``Raw options``: Raw ``ansible-playbook`` options
- ``Playbook command``: Path to ``ansible-playbook`` command
- ``Working directory``: Working directory to use


Galaxy
------

Install dependencies using ``ansible-galaxy`` (*TODO*).


============
Installation
============

Use *Preferences->Plugins->Install Plugin from Disk...* to install a local
copy of the plugin zip file from ``build/distributions``.


.. _travis: https://travis-ci.org/mdklatt/idea-ansible-plugin
.. _Ansible: https://docs.ansible.com/ansible/latest/index.html
.. _Ansible logo: https://www.ansible.com/logos
.. _JetBrains: https://www.jetbrains.com

.. |copy| unicode:: U+000A9 .. COPYRIGHT SIGN
.. |travis| image:: https://travis-ci.org/mdklatt/idea-ansible-plugin.png
   :alt: Travis CI build status
   :target: `travis`_
