pipeline {
    agent { label 'docker' }
    environment {
        PROJECT_VERSION = readMavenPom().getVersion()
    }
    
    stages {
        stage('run tests') {
                
            steps {
                echo "branch ${env.BRANCH_NAME}" 
                echo "${PROJECT_VERSION}"

                withMaven(
                    mavenSettingsConfig: '67c40a88-505a-4f78-94a3-d879cc1a29f6'
                ) {
                    sh 'mvn -U install' 
                }
            }
        }
        stage('snapshot deploy') {
            when {
                expression { PROJECT_VERSION ==~ /.*-SNAPSHOT/ }
            }
            steps {
                echo "deploying version ${PROJECT_VERSION}"
                withMaven(
                    mavenSettingsConfig: '67c40a88-505a-4f78-94a3-d879cc1a29f6'
                ) {
                    sh 'mvn deploy -Dmaven.test.skip=true || echo $?'
                }
            }
        }
        stage('release deploy') {
            when {
                allOf {
                    branch 'main'
                    not { expression { PROJECT_VERSION ==~ /.*-SNAPSHOT/ } }
                    anyOf {
                        changeset 'pom.xml'
                        changeset 'src/*'
                    }
                }
            }
            steps {
                echo "deploying release version ${PROJECT_VERSION}"
                
                withMaven(mavenSettingsConfig: '67c40a88-505a-4f78-94a3-d879cc1a29f6') {
                    sh 'mvn deploy -Dmaven.test.skip=true'
                }
                
                sshagent (credentials: ['cxp-bot']) {
                    sh "git tag v$PROJECT_VERSION"
                    sh 'git push --tags || echo "WARN: could not push tags, but build succeeded"'
                }
            }
        }
    }
    post {
        success {
            setBuildStatus("Build succeeded", "SUCCESS")
        }
        failure {
            setBuildStatus("Build failed", "FAILURE")
        }
    }
}

def getRepoURL() {
  sh "git config --get remote.origin.url > .git/remote-url"
  return readFile(".git/remote-url").trim()
}
 
def getCommitSha() {
  sh "git rev-parse HEAD > .git/current-commit"
  return readFile(".git/current-commit").trim()
}

void setBuildStatus(String message, String state) {
  repoUrl = getRepoURL()
  commitSha = getCommitSha()

  step([
      $class: "GitHubCommitStatusSetter",
      reposSource: [$class: "ManuallyEnteredRepositorySource", url: repoUrl],
      commitShaSource: [$class: "ManuallyEnteredShaSource", sha: commitSha],
      errorHandlers: [[$class: "ChangingBuildStatusErrorHandler", result: "UNSTABLE"]],
      statusResultSource: [ $class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: message, state: state]] ]
  ]);
}
