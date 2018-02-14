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
  job = build job: name, parameters: parameters, propagate: false
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
