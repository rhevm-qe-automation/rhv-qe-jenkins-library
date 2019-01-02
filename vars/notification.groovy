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
    to: rhv-qe-devops, \
    body: """
    Hi infra-team,

    ${mail_body}

    Your RHV-QE ${version} testing pipeline.
    """
}

def notificator(failed_build, failed_job) {
  def failed_stage = other.parse_url(failed_build)
  def build_base_name = other.parse_url(failed_job, true)
  def stage_link = "<a href=${failed_build}"+"consoleFull"+">${failed_stage}</a>"
  def job_link = "<a href=${failed_job}"+"consoleFull"+">${build_base_name}</a>"
  def retrigger_link = "<a href=${failed_build}"+"rebuild/parameterized"+">here</a>"
  def team_email = other.get_team_email(failed_build)
  def recipient = team_email.contains("rhv-qe-devops")? team_email : "${team_email},rhv-qe-devops"

  mail subject: """[Action Required]: ${failed_stage} has FAILED!""", \
   mimeType: 'text/html', \
   to: recipient, \
   replyTo: "rhv-qe-devops", \
   body: """
   Hi, <br>
   The following job: ${stage_link} has <font color="red"><b>FAILED</b></font> in stage:
   ${job_link}. <br>
   You can directly re-trigger ${retrigger_link}.<br>
   Please investigate root cause of the job failure. In case it’s infra related issue, please inform rhv-qe-devops team
   in reply to this email.
   """
}
