# Build an Alpine image capable of running Ansible installed from a Python
# virtualenv at /opt/ansible.
FROM linuxserver/openssh-server:version-9.3_p2-r0
RUN \
  apk add gcc libffi-dev musl-dev python3 python3-dev && \
  python3 -m venv /opt/ansible  && \
  . /opt/ansible/bin/activate && \
  python3 -m pip install ansible==6.1.0
