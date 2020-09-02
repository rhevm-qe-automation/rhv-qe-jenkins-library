package rhvqe

/**
 * Sends email notification about job about to be started and appends job to job map.
 *
 * @param args Map Object containing the following keys.
 *
 * Mail related arguments:
 *   args.jobMailSubject: Subject of the email to be sent
 *   args.jobVersion: RHV Version, Ex. 4.2
 *   args.jobCurrentBuildUrl: Current pipeline build URL.
 *   args.jobName: Helper name of the job about to be started. Ex. Tier1-API
 *
 * Pipelines jobs group related arguments:
 *   args.jobResultsMap: Map with jobName -> current_job mapping
 *   args.jobMap: Map of job that should be run in parallel
 *
 * Current Job Execution related arguments:
 *   args.jobRunParam: true if Job should be executed. False otherwise
 *   args.jobUrl: Job name in Jenkins. Ex. rhv-ge-4.2-flow-infra
 *   args.jobParameters: Parameters to be passed to the job.
 *   args.jobStable: If false job is handled as not stabled. In this case the pipeline will conitnue even if it failed.
 *   args.jobDelay: If you want to delay of the execution (Good for ordering).
 */
def createNewJob(args)
{
    def jobClusure = {
        _args ->
        def _jobMailBody = "Scheduling ${_args.jobName} flow,\nPlease follow ${_args.jobCurrentBuildUrl} to get more info."
        notification.send_notification(
          _args.jobMailSubject,
          _jobMailBody,
          _args.jobMailRecipients,
        )
        def jobDelay = args.jobDelay ?: 0
        sleep jobDelay
        def current_job = jobs.build_job(
          _args.jobUrl, _args.jobParameters, _args.jobStable
        )
        if (current_job){
          _args.jobResultsMap[_args.jobName] = jobs.get_job_url(current_job)
        }
    }
    jobs.appendJob(args.jobMap, args.jobName, args.jobRunParam, jobClusure, args)
}
