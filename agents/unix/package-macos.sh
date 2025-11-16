#!/bin/zsh


export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-jdk-21.0.9+7.1/Contents/Home
export PATH=/Library/Java/JavaVirtualMachines/graalvm-jdk-21.0.9+7.1/Contents/Home/bin:$PATH

mvn -Pnative native:compile -DmainClass=com.spulido.agent.AgentApplication
