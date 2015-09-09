#!/usr/bin/env bash

#
# Wrapper script for initializing the runtime environment before running 
# a GenePattern module on a compute node.
#
# Usage: run-with-env.sh [-c <env-custom-site.sh>] -u <env-id.0> -u <env-id.1> ... -u <env-id.N> <cmd> [<args>]
# Each '-u' flag declares a module runtime environment which must be initialized.
# For Broad hosted servers this corresponds to a dotkit name.
#
# Configuration
# Create a new 'env-custom.sh' script as a copy of the 'env-default.sh' file.'
# Modify as needed for your GP instance.
#
# Extra features
# The optional '-c' flag can set an alternate name for the customization file.
# It must be the first arg to the script.
# E.g. 
#     ./run-with-env.sh -c env-custom-broad-centos5.sh -u R-3.1 echo "Hello" 
#
#
# The GP_ENV_CUSTOM environment variable can also be used to set an alternate name for the
# customization file.
# E.g.
#     export GP_ENV_CUSTOM="env-custom-broad-centos5.sh"; ./run-with-env.sh -u R-3.1 echo "Hello"
#
 
script_dir=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source "$script_dir/env-lookup.sh"
sourceEnvDefault

# special-case: check for -c <gp_env_custom> 
if [ "$1" = "-c" ]; then
    shift;
    sourceEnvCustom "$1"
    shift;
else
    sourceEnvCustom
fi

idx=0
while getopts u: opt "$@"; do
    addEnv "$OPTARG"
    idx=$((idx+1))
    # need this line to remove the -u <module> from the cmdline
    shift $((OPTIND-1)); OPTIND=1
done

for module in "${_runtime_environments[@]}"
do
  initEnv "${module}"
done

# add this directory to the path
PATH="${script_dir}:${PATH}"

# run the command
"${@}" 
