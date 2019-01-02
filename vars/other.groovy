def get_locked_resources() {
  return org.jenkins.plugins.lockableresources.LockableResourcesManager.class.get().getResourcesFromBuild(currentBuild.getRawBuild())
}

def get_locked_ge_name() {
  return get_locked_resources()[0].getName()
}

def get_locked_ge_labels() {
  return get_locked_resources()[0].getLabels()
}

def get_resources_with_label(label){
  return org.jenkins.plugins.lockableresources.LockableResourcesManager.class.get().getResourcesWithLabel(label, null)
}

def get_resource_labels(label){
  return get_resources_with_label(label)[0].getLabels()
}

def is_he_env(env_name) {
  return get_resource_labels(env_name).contains("he-env")
}

def get_username() {
  def cause = currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause)
  if(!cause) // wasn't triggered by user directly
    return "";
  else
    return cause.userId;
}

def parse_url(failed_job, is_stage=false) {
  def tokens = failed_job.split('/')
  if (is_stage) {
    return tokens[-2]
  }
  return tokens[-2] + "/" + tokens[-1]
}

def get_team_email(url) {
  if(url.contains('network')){
    return "rhevm-qe-network@redhat.com"
  }
  if(url.contains('compute')){
    return "rhevm-qe-compute@redhat.com"
  }
  if(url.contains('storage')){
    return "rhevm-qe-storage@redhat.com"
  }
  if(url.contains('coresystem')){
    return "rhevm-qe-infra@redhat.com"
  }
  return "rhv-qe-devops@redhat.com"
}
