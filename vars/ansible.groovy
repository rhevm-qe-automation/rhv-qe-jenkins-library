// vars/ansible.groovy
def call(Map config = [:]) {
    // Any valid steps can be called from this code, just like in other
    // Scripted Pipeline
    def toolPath = "venv-ansible"
    def playbook = config.get('playbook')
    def inventory = config.get('inventory', "inventory")
    def tags = config.get('tags', [])
    def extraVars = config.get('extraVars', [])
    def gitUrl = config.get('gitUrl', 'https://github.com/petr-balogh/venv-ansible.git')
    def gitBranch = config.get('gitBranch', 'master')
    def ansibleParams = ""
    if (tags) {
      ansibleParams += "--tags ${tags.join(',')}"
    }
    extraVars.each {
      ansibleParams += " -e ${it}"
    }
    echo "All assible params: ${ansibleParams}"
    dir (toolPath) {
      git url: "${gitUrl}", branch: "${gitBranch}"
      ansiColor('xterm') {
        sh "${WORKSPACE}/${toolPath}/venv-ansible ${inventory} ${playbook} $ansibleParams"
      }
    }
}
