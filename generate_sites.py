import yaml
import sys
import os


def deep_key_update(dict_to_update: dict, key_path: list, value) -> dict:
    key = key_path.pop(0)
    if len(key_path) == 0:
        dict_to_update[key] = value
        return dict_to_update
    else:
        return deep_key_update(dict_to_update[key], key_path, value)


template_yaml_directory = 'template_yaml'
k8_directory = '.'
level_sites = int(sys.argv[1])

# Read the Template YAML file
with open(os.path.join(template_yaml_directory, 'level_site_service_template.yaml'), 'r') as fd:
    level_site_service = yaml.safe_load(fd)

with open(os.path.join(template_yaml_directory, 'level_site_deployment_template.yaml'), 'r') as fd:
    level_site_deployment = yaml.safe_load(fd)

all_level_site_domains = []
ports = []
# Create a list of all level-site domains, used for client and server
for level in range(1, level_sites):
    level_site = f"ppdt-level-site-{level:02}-service"
    all_level_site_domains.append(level_site)
    port_number = 9000 + level - 1
    ports.append(str(port_number))
all_domains_env = {'name': 'LEVEL_SITE_DOMAINS', 'value': ','.join(all_level_site_domains) }

# Update all values for service - LEVEL-SITE
for level in range(1, level_sites):
    # Update the name
    level_site_service['metadata']['name'] = f'ppdt-level-site-{level:02}-service'
    level_site_service['spec']['selector']['pod'] = f'ppdt-level-site-{level:02}-deploy'

    # Create new file
    current_level_site_service_file = f"level_site_{level:02}_service.yaml"
    with open(os.path.join(k8_directory, current_level_site_service_file), 'w') as fd:
        yaml.dump(level_site_service, fd)

# Update all values for deployment - LEVEL-SITE
for level in range(1, level_sites):
    # Update the name
    current_level_site_deploy_name = f'ppdt-level-site-{level:02}-deploy'
    level_site_deployment['metadata']['name'] = current_level_site_deploy_name
    level_site_deployment['metadata']['labels']['app'] = current_level_site_deploy_name
    level_site_deployment['spec']['selector']['matchLabels']['pod'] = current_level_site_deploy_name
    level_site_deployment['spec']['template']['metadata']['labels']['pod'] = current_level_site_deploy_name
    deep_key_update(level_site_deployment, ['spec', 'template', 'spec', 'containers', 0, 'name'],
                    current_level_site_deploy_name)

    # Create a new file
    current_level_site_deploy_file = f"level_site_{level:02}_deploy.yaml"
    with open(os.path.join(k8_directory, current_level_site_deploy_file), 'w') as fd:
        yaml.dump(level_site_deployment, fd)

# -------------------------------Update Client Template--------------------------------------------
with open(os.path.join(template_yaml_directory, 'client_deployment_template.yaml'), 'r') as fd:
    client_site_deployment = yaml.safe_load(fd)

deep_key_update(client_site_deployment, ['spec', 'template', 'spec', 'containers', 0, 'env', 5],
                all_domains_env)

with open(os.path.join(k8_directory, 'client_deployment.yaml'), 'w') as fd:
    yaml.dump(client_site_deployment, fd)

# -------------------------------Update Server Template-------------------------------------------
with open(os.path.join(template_yaml_directory, 'server_site_deployment_template.yaml'), 'r') as fd:
    server_site_deployment = yaml.safe_load(fd)

deep_key_update(server_site_deployment, ['spec', 'template', 'spec', 'containers', 0, 'env', 3],
                all_domains_env)

with open(os.path.join(k8_directory, 'server_site_deployment.yaml'), 'w') as fd:
    yaml.dump(client_site_deployment, fd)

# -------------------------------Might as well Update Properties File-----------------------------
from configobj import ConfigObj
config = ConfigObj("config.properties")
config['level-site-ports'] = ','.join(ports)
config.write()