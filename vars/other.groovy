def get_locked_ge_name() {
  return org.jenkins.plugins.lockableresources.LockableResourcesManager.class.get().getResourcesFromBuild(currentBuild.getRawBuild())[0].getName()
}


def get_username() {
  def cause = currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause)
  if(!cause) // wasn't triggered by user directly
    return "";
  else
    return cause.userId;
}
