import groovy.json.JsonSlurper


/**
* Try to figure out to which build the rhvh belongs to
*
* @param job_description String contains the RHVH build id like RHVH-4.2-20190303.0
*
* @return build String build of rhv like 4.3.3-2
*/
def rhvh_build_version(job_description) {
    def ver = job_description.find("[0-9]+.[0-9]+")
    def rhevm_qe_infra_dir = "${WORKSPACE}/rhevm-qe-infra"
    def build = sh (
      script: "${rhevm_qe_infra_dir}/scripts/misc/get-compose-for-rhvh.sh ${ver}",
      returnStdout: true
    ).trim()
    return (build == 'None') ? 'unknown' : build
}

/**
* Clone the rhevm-qe-infra repository to workspace
**/
def clone_infra_repo() {
    // Define rhevm-qe-infra repo
    def rhevm_qe_infra_url = "https://gitlab.cee.redhat.com/rhevm-qe-automation/rhevm-qe-infra.git"
    def rhevm_qe_infra_dir = "${WORKSPACE}/rhevm-qe-infra"
    sh "rm -rf ${rhevm_qe_infra_dir} && git clone ${rhevm_qe_infra_url}"
}

/**
 * Extracts the displayName from given build.
 *
 * @param response String Object containing the response from previous HTTP GET request.
 * @param parent_pipeline_status String Object indicates whether this is an parent build.
 *
 * @return displayName String represents the displayName of the build.
 */
def build_info(response, parent_pipeline_status =null) {
    def response_object = null

    try {
      def jsonSlurper = new JsonSlurper()
      response_object = jsonSlurper.parseText(response)

      if (parent_pipeline_status) {
        def description = response_object.description

        if (!description) {
          // Executed only in case of pending job, i.e. 'waiting' status
          response_object.actions.any { action ->
            if (action.containsKey('parameters')) {
              action.parameters.any { param ->
                if (param['name'].equals("TEXT_BUILD_DESCRIPTION")) {
                  description = param['value']
                  return true
                }
              }
              return true
            }
          }
        }
        return [(description) ? description.trim() : "Unknown" , null]  // row in google worksheet
      }

      // Create URL towards parent build
      def url = env.JENKINS_URL \
             + response_object.actions.causes[0].upstreamUrl[0] \
             + response_object.actions.causes[0].upstreamBuild[0] \
             + "/api/json"

      response = url.toURL().text
      response_object = jsonSlurper.parseText(response)
    } catch (Error e){}
    def buildType = (response_object.description) ? response_object.description.split(':')[1].trim() : ""
    return [response_object.displayName, buildType] // name of google worksheet and strategy
}

/**
 * Monitors and updates the status of production jobs/builds.
 *
 * @param config Map Object containing the following keys.
 *
 * Mail related arguments:
 *   config.job_parameters: Parameters of given build/job
 *   config.name: Name of the job/build
 *   config.update_status: Indicate whether the parent build is in WAITING or ABORTED status.
 */
def call(Map config = [:]) {
    ansiColor('xterm') {
    def job_parameters = config.get('job_parameters')
    def job_name = config.get('name')
    def cherry_pick = config.get('ref')
    def build_status = config.get('update_status')

    // Defining URL for REST API request
    def url = env.BUILD_URL + "api/json"
    def response = url.toURL().text

    clone_infra_repo()
    def rhevm_qe_infra_dir = "${WORKSPACE}/rhevm-qe-infra"
    def rhevm_qe_infra_url = "https://gitlab.cee.redhat.com/rhevm-qe-automation/rhevm-qe-infra.git"

    // do the cherry-pick
    if (cherry_pick) {
      def refs = cherry_pick.tokenize(' ')
      refs.each {
        sh """
          if [ -d ${rhevm_qe_infra_dir} ]; then
            pushd ${rhevm_qe_infra_dir}
            git fetch ${rhevm_qe_infra_url} ${it} && git checkout FETCH_HEAD || (
                echo '!!! FAIL WHILE CHECKOUT' ${it} ; false
            )
            popd
          fi
        """
      }
    }

    def (build_name, strategy)  = build_info(response)
    if (build_name.contains("RHVH")) {
      build_name = "rhv-" + rhvh_build_version(build_name)
    }

    // Check if the build is upstream-build
    if (build_status) {
      def (description, dummy) = build_info(response, build_status)
      def strategy_arg = strategy ? "${strategy}" : ""  // No strategy - no Jira ticket
      sh """
        ${rhevm_qe_infra_dir}/scripts/production-monitoring/pygsheets-env.sh \
          build_version=${build_name} \
          build_description=${description} \
          build_status=${build_status} \
          build_url=${env.BUILD_URL} \
          strategy=${strategy_arg} \
          stage=init
      """
      return
    }

    // Update GE Execution Sheet with pre-build configuration
    sh """
      ${rhevm_qe_infra_dir}/scripts/production-monitoring/pygsheets-env.sh \
        build_version=${build_name} \
        build_description=${currentBuild.description} \
        build_url=${env.BUILD_URL} \
        job_name=${job_name} \
        stage=before_job
    """
    def build_result = build job: job_name, parameters: job_parameters, propagate: false, wait: true

    // Update GE Execution Sheet with post-build configuration
    sh """
      ${rhevm_qe_infra_dir}/scripts/production-monitoring/pygsheets-env.sh \
        build_version=${build_name} \
        build_description=${currentBuild.description} \
        build_url=${env.BUILD_URL} \
        job_name=${job_name} \
        build_status=${build_result.result} \
        stage=after_job
    """
    return build_result
    }
}
