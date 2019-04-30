/**
 * The Pip Detector can discover dependencies of Python projects.
 * <p>
 * The Pip Detector will attempt to run on your project if either a setup.py file is found,
 * or a requirements.txt file is provided via the --detect.pip.requirements.path property.
 * </p>
 * <p>
 * The Pip Detector also requires python and pip executables.
 * </p>
 * <p>
 * The Pip Detector runs the pip-inspector.py script, which uses python/pip
 * libararies to query the pip cache for the project (which may or may not
 * be a virtual environment) for dependency information:
 * <ol>
 *     <li>pip-inspector.py queries for the project dependencies by project
 *     name (which can be discovered using setup.py, or provided via the
 *     detect.pip.project.name property) using the
 *     <a href="https://setuptools.readthedocs.io/en/latest/pkg_resources.html">pkg_resources library</a>.
 *     If your project has been installed into the pip cache,
 *     this will discover dependencies specified in setup.py.</li>
 *     <li>If a requirements.txt file was provided, pip-inspector.py
 *     uses a python API, parse_requirements, to query the requirements.txt
 *     file for possible additional dependencies, and uses the pkg_resources
 *     library to query for the details of each one. (The parse_requirements
 *     API is unstable, leading to the decision to deprecate this detector.)</li>
 * </ol>
 * </p>
 * <p>
 * Ramifications of this approach:
 * <ul>
 *     <li>Because pip-inspector.py uses the pkg_resources library to discover dependencies,
 *     only those packages which have been installed into the pip cache will be included
 *     in the output. Additional details are available in the
 *     <a href="https://setuptools.readthedocs.io/en/latest/pkg_resources.html">setuptools doc</a>.</li>
 * </ul>
 * </p>
 * <p>
 * Recommendations:
 * <ul>
 *     <li>Plan to migrate to Pipenv, since this detector is deprecated</li>
 *     <li>Create a setup.py file</li>
 *     <li>Install your project into the pip cache</li>
 *     <li>If there are any dependencies specified in requirements.txt that are not specified in setup.py, provide the requirements.txt file</li>
 * </ul>
 * </p>
 */
package com.synopsys.integration.detectable.detectables.pip;