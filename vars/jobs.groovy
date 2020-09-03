def appendJob(jobMap, jobName, jobRunParam, jobDefinition, jobArgs){
  jobMap[jobName] = {
    stage(jobName){
      jobRunParam.toBoolean() ? jobDefinition.call(jobArgs)
                              : echo("Skipped job ${jobName}")
    }
  }
}

def get_job_url(job){
    return "${env.JENKINS_URL}/" + job.getRawBuild().getUrl()
}

def build_job(name, parameters, is_stable)
{
  def job = null
  job = build job: name, parameters: parameters, propagate: false, wait: true
  if(is_stable)
  {
    if(job.getResult().contains("SUCCESS"))
    {
      echo "${name} job ended with success!!!"
    }
    else if (job.getResult().contains("UNSTABLE"))
    {
      currentBuild.result = job.getResult()
    }
    else  // ABORTED, FAILURE, ...
    {
        error("${name} job finished as ${job.result} ... interrupting pipeline")
    }
  }
  else {
    if(!(job.getResult().contains("SUCCESS") || job.getResult().contains("UNSTABLE")))
    {
      echo "${name} failed, it is not stable ... ignoring."
    }
  }
  return job
}

def build_and_notify(name, parameters, cherry_pick, ignore_unstable=false){
  def current_job = null
  def cause = currentBuild.rawBuild.getCause(hudson.model.Cause$UpstreamCause).toString()
  if (cause && (cause.contains('production-testing-pipeline') || cause.contains('rhvh-pipeline'))) {
    current_job = job_monitoring(ref: cherry_pick, job_parameters: parameters, name: name, update_status: null)
  }
  else {
    current_job = build job: name, parameters: parameters, propagate: false, wait: true
  }

  if (current_job.result != 'SUCCESS') {
    if (env.NODE_NAME.contains('production')) {
      if (ignore_unstable && current_job.result == 'UNSTABLE')
        return

      notification.failed_job_notification(env.BUILD_URL, get_job_url(current_job))
    }
    throw new Exception(other.parse_url(get_job_url(current_job), true) + " has TERMINATED with status " + current_job.result)
  }
}
