#!/usr/bin/python3

import shutil
import os
import subprocess

import click


def wsl_path(path):
    stdout = subprocess.check_output(['wslpath', '-w', os.path.abspath(path)])
    stdout = stdout.decode('utf-8')
    stdout = stdout.strip()
    return stdout


@click.command()
@click.argument('tcl', type=click.Path(dir_okay=False, exists=True))
@click.argument('project', type=click.Path(exists=False))
@click.option('--remove/--no-remove', default=False)
@click.option('-y/', '--yes/--confirm`', default=False)
def main(tcl, project, remove, yes):

    if os.path.isdir(project) and remove and (yes or click.confirm(f'Remove {project}')):
        print(f'Removing {project}')
        shutil.rmtree(project)

    tcl = wsl_path(tcl)
    project = wsl_path(project)

    print(f'TCL: {tcl}')
    print(f'PROJECT: {project}')

    subprocess.call(
        [
            "cmd.exe",
            "/C",
            'C:/Xilinx/Vivado/2023.2/bin/vivado.bat',
            '-mode', 'batch',
            '-source', tcl,
            '-tclargs', project
        ],
        cwd="/mnt/c"
    )


if __name__ == '__main__':
    main()
