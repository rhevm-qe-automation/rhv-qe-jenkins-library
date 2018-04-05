def get_locked_resources() {
  return org.jenkins.plugins.lockableresources.LockableResourcesManager.class.get().getResourcesFromBuild(currentBuild.getRawBuild())
}

def get_locked_ge_name() {
  get_locked_resources()[0].getName()
}

def get_locked_ge_labels() {
  get_locked_resources()[0].getLabels()
}

def is_he_env() {
  get_locked_ge_labels().contains("he-env")
}

def get_username() {
  def cause = currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause)
  if(!cause) // wasn't triggered by user directly
    return "";
  else
    return cause.userId;
}
