// vars/v2v_ansible.groovy
def call(Map config = [:]) {
    // Any valid steps can be called from this code, just like in other
    // Scripted Pipeline
    def playbook = config.get('playbook')
    def inventory = config.get('inventory', "jenkins/qe/v2v/inventory")
    def tags = config.get('tags', [])
    def extraVars = config.get('extraVars', [])
    def ansibleParams = ""
    if (tags) {
      ansibleParams += "--tags ${tags.join(',')}"
    }
    extraVars.each {
      ansibleParams += " -e ${it}"
    }
    echo "All ansible params: ${ansibleParams}"
    dir ('venv-ansible') {
      ansiColor('xterm') {
        sh "${WORKSPACE}/venv-ansible/venv-ansible ${inventory} ${playbook} $ansibleParams"
      }
    }
}
