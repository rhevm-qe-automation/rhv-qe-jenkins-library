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
