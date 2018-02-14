class ResourceLocker implements Serializable{{

  def jobQueueList = []
  def resourceList = []
  def resourcesMap = [:]
  def freeResourcesMap = [:]
  def lockedRsourcesMap = [:]
  def fatalErrorOccured = false
  def fatalErrorEx
  def script
  def holdResources

  ResourceLocker(script, Integer holdResources=0){{
    this.script = script
    // holdResources is used when you want to hold some of resources and don't
    // force release them when in queue won't be anything waiting. This is
    // used if you want to run for example aggregator on same resource after
    // parallel run.
    this.holdResources = holdResources
  }}

  def setResources(){{
    def _resourcesMap = [:]
    this.resourceList = getLockedResources()
    this.resourceList.each{{
      _resourcesMap[it.getName()] = it.getLabels().split()
    }}
    if(!freeResourcesMap){{
      this.freeResourcesMap = _resourcesMap.clone()
      this.resourcesMap = _resourcesMap.clone()
    }}
  }}

  def findResourceWithLabel(label){{
    def returnValue = [null, null]
    freeResourcesMap.each{{ k, v ->
      v.each{{
        if (it == label){{
          returnValue = [k, v]
          return true
        }}
      }}
    }}
    return returnValue
  }}

  def lockFreeResource(resourceLabel, failJobs=true){{
    while (true) {{
      if (!(fatalErrorOccured && failJobs)){{
        if (freeResourcesMap.size() > 0){{
          // https://issues.jenkins-ci.org/browse/JENKINS-38846
          // W/A before tuples will be solved
          def foundResource = findResourceWithLabel(resourceLabel)
          def resourceNameForLock = foundResource[0]
          def resourceLabels = foundResource[1]
          if (resourceNameForLock){{
            if (freeResourcesMap.containsKey(resourceNameForLock)){{
              freeResourcesMap.remove(resourceNameForLock)
              lockedRsourcesMap[resourceNameForLock] = resourceLabels
              script.echo("Locking resource: ${{resourceNameForLock}} with labels ${{resourceLabels}}")
              return resourceNameForLock
            }}
          }}
        }}
        // Sleep 1 sec because we run this loop to check status of locked
        // resources. Here we need to have some delay before another check.
        sleep(1000)
      }} else {{
        script.echo("Cannot lock resource! Fatal error occured ${{fatalErrorEx}}")
        throw this.fatalErrorEx
      }}
    }}
  }}

  def checkUnusedResources(){{
    // _resourcesMap contain all resources from which we will remove all which
    // are already locked or will be used in future (queue of jobs)
    def _resourcesMap = resourcesMap.clone()
    if (_resourcesMap.size() <= holdResources){{
      return true
    }}
    // removing of resources which are locked at the moment
    lockedRsourcesMap.each{{
      _resourcesMap.remove(it)
    }}
    // Check if resource's label is going to be used
    jobQueueList.find{{ label ->
      def resourcesToRemove = []
      _resourcesMap.find{{ resourceName, resourceLabels ->
        if (label in resourceLabels){{
          // if it's going to be used we won't delete it, so we will put it
          // to resourcesToRemove and in next step we will remove this resource
          // from from _resourcesMap
          resourcesToRemove.push(resourceName)
          return true
        }}
      }}
      if (!_resourcesMap){{
        // if there is no resource, it means that they will be used (no need to
        // release them yet)
        return true
      }}
      for (resName in resourcesToRemove){{
        _resourcesMap.remove(resName)
      }}
    }}
    _resourcesMap.each{{
      resourceList.each{{ resource ->
        if (it.key == resource.getName()){{
          // resources will be removed in releaseResources from
          // resourcesMap and freeResourcesMap
          releaseResource(resource)
        }}
      }}
    }}
  }}

  def unlockResource(resourceName){{
    def resourceLabels = lockedRsourcesMap[resourceName]
    lockedRsourcesMap.remove(resourceName)
    script.echo("Unlocking GE: ${{resourceName}} with labels ${{resourceLabels}}")
    freeResourcesMap[resourceName] = resourceLabels
  }}

  def lockResource(resourceLabel, failJobs=true, closure){{
    if(!resourceList){{
      setResources()
    }}
    jobQueueList.push(resourceLabel)
    def resourceName = lockFreeResource(resourceLabel, failJobs)
    jobQueueList.remove(resourceLabel)
    if (resourceName){{
      try{{
        closure(resourceName)
      }} catch(Exception ex){{
        if (failJobs){{
          this.fatalErrorOccured = true
          this.fatalErrorEx = ex
          script.echo("Exception cought: ${{ex}}")
        }}
      }} finally{{
        unlockResource(resourceName)
        checkUnusedResources()
      }}
    }}
  }}

  def getLockedResources(){{
    return org.jenkins.plugins.lockableresources.LockableResourcesManager.class.get().getResourcesFromBuild(script.currentBuild.getRawBuild())
  }}

  def releaseResource(resource){{
    script.echo("Force releasing GE resource: ${{resource}}")
    resourcesMap.remove(resource.getName())
    freeResourcesMap.remove(resource.getName())
    org.jenkins.plugins.lockableresources.LockableResourcesManager.class.get().unlock([resource], script.currentBuild.getRawBuild())
  }}
}}
