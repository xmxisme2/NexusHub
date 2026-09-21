pipeline {
  agent any

  options {
    disableConcurrentBuilds()
    timeout(time: 45, unit: "MINUTES")
    buildDiscarder(logRotator(numToKeepStr: "10"))
  }

  stages {
    stage("构建并重启 NexusHub") {
      steps {
        sh """
          set -eu
          ssh \\
            -i /var/jenkins_home/.ssh/opsdesk_deploy \\
            -o BatchMode=yes \\
            -o IdentitiesOnly=yes \\
            -o UserKnownHostsFile=/var/jenkins_home/.ssh/known_hosts \\
            jenkins-deploy@host.docker.internal deploy-nexushub ${GIT_COMMIT}
        """
      }
    }
  }
}
