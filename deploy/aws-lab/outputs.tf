output "docker_lab_public_ip" {
  description = "Public IP of the docker-lab host (runs the full lab/ compose)."
  value       = aws_instance.docker_lab.public_ip
}

output "docker_lab_ssh" {
  description = "SSH command for the docker-lab host."
  value       = "ssh ubuntu@${aws_instance.docker_lab.public_ip}"
}

output "docker_lab_service_urls" {
  description = "Reachable lab service endpoints on the docker-lab host (from allowed CIDRs only)."
  value = {
    drupal     = "http://${aws_instance.docker_lab.public_ip}:8081"
    tomcat     = "http://${aws_instance.docker_lab.public_ip}:8082"
    flask      = "http://${aws_instance.docker_lab.public_ip}:8000"
    thinkphp   = "http://${aws_instance.docker_lab.public_ip}:8083"
    docker_api = "http://${aws_instance.docker_lab.public_ip}:2375"
    nodejs     = "http://${aws_instance.docker_lab.public_ip}:3000"
  }
}

output "target_vm_public_ips" {
  description = "Public IPs of the standalone target VMs."
  value       = aws_instance.target_vm[*].public_ip
}

output "target_vm_ssh" {
  description = "SSH commands for the target VMs."
  value       = [for i in aws_instance.target_vm : "ssh ubuntu@${i.public_ip}"]
}

output "all_target_ips" {
  description = "Every target IP (docker-lab + VMs) to register in the central platform."
  value       = concat([aws_instance.docker_lab.public_ip], aws_instance.target_vm[*].public_ip)
}
