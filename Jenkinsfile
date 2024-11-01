#!groovy
mavenOpts = [
        '-DproxySet=true',
        '-Dhttp.proxyHost=192.168.221.12',
        '-Dhttp.proxyPort=3128',
        '-Dhttp.nonProxyHosts="localhost|127.0.0.1|172.16.*.*|172.18.*.*|10.*.*.*|192.168.*.*|metadata.google.internal|169.254.169.254|*.labs.optiva.com|redknee.com|.redknee.com|*.redknee.com"',
        '-Dhttps.proxyHost=192.168.221.12',
        '-Dhttps.proxyPort=3128',
        '-Dhttps.nonProxyHosts="localhost|127.0.0.1|172.16.*.*|172.18.*.*|10.*.*.*|192.168.*.*|metadata.google.internal|169.254.169.254|*.labs.optiva.com|redknee.com|.redknee.com|*.redknee.com"',
].join(' ')
uc.nodeSize('huge')
uc.addJobParam(stringParam(
        name: 'REL_VERSION', defaultValue: '', description: 'Specific version to create release for')
)
uc.addJobStage(1, 'Set version') {
    env.MAVEN_OPTS=mavenOpts
    if (env.REL_VERSION) {
        sh "scripts/update-versions.sh $env.REL_VERSION"
    } else {
        echo "Not setting version"
    }
}
uc.addJobStage(2, 'Build') {
    sh "./mvnw -s settings.xml package -DskipTests=true -Dmaven.javadoc.skip=true"
}
uc.addJobStage(3, 'Deploy') {
    if (env.REL_VERSION) {
        withCredentials([usernamePassword(
                credentialsId: 'ARTIFACTORY_PUBLISH',
                passwordVariable: 'server.password',
                usernameVariable: 'server.username')]) {
            try {
                sh "./mvnw -s settings.xml deploy -DskipTests=true -Dmaven.javadoc.skip=true"
            } catch (e) {
                echo "Deploy failed with exception $e"
            }
        }
    } else {
        echo "Skipping deploy"
    }
}

uc.customPipeline()