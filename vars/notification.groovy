def send_notification(subject, mail_body, version){
  mail subject: subject, \
    to: "rhevm-qe-automation", \
    body: """
    Hi,

    ${mail_body}

    Your RHV-QE ${version} testing pipeline.
    """
}


def send_notification_infra(subject, mail_body, version){
  mail subject: subject, \
    to: "rhev-qe-infra", \
    body: """
    Hi infra-team,

    ${mail_body}

    Your RHV-QE ${version} testing pipeline.
    """
}
