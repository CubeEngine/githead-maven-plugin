/*
 * The MIT License
 * Copyright © 2013 Cube Island
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.cubeengine.maven.plugins.githead.mojo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.plugins.annotations.LifecyclePhase.INITIALIZE;

/**
 * This mojo adds the git head hash as a variable
 */
@Mojo(name = "head", defaultPhase = INITIALIZE, threadSafe = true)
public class HeadMojo extends AbstractMojo
{
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project = null;

    @Parameter(defaultValue = "${project.basedir}")
    private File repoLocation = null;

    @Parameter
    private boolean searchParentDirectories = true;

    @Parameter
    private String defaultBranch = "unknown";

    @Parameter
    private String defaultCommit = this.defaultBranch;

    @Parameter
    private boolean failOnFailure = false;

    private static final String HEAD_FILE_NAME = "HEAD";
    private static final String GIT_REF_PREFIX = "ref:";
    private static final String GIT_FOLDER = ".git";

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if (this.repoLocation == null)
        {
            failOrWarn("No repo location given!", null);
            this.repoLocation = new File(".");
        }
        Path searchBase = this.repoLocation.toPath().toAbsolutePath();
        getLog().info("Default location: " + searchBase.toString());

        Path repo = searchGitRepository(searchBase, searchParentDirectories);
        if (repo == null)
        {
            failOrWarn("Failed to find a git repository going upwards from: " + searchBase, null);
            setProperties(defaultBranch, defaultCommit);
        }
        else
        {
            try
            {
                final String head = readFile(repo.resolve(HEAD_FILE_NAME));
                final String branch = headToBranch(head, defaultBranch);
                final String commit = dereference(repo, head);
                setProperties(branch, commit);
            }
            catch (IOException e)
            {
                failOrWarn("Failed to resolve branch and/or commit information from HEAD.", e);
                setProperties(defaultBranch, defaultCommit);
            }
        }
    }

    private void setProperties(String branch, String commit)
    {
        getLog().info("Found: " + branch + "/" + commit);
        Properties properties = this.project.getProperties();
        properties.put("githead.branch", branch);
        properties.put("githead.commit", commit);
    }

    private void failOrWarn(String message, Throwable cause) throws MojoFailureException
    {
        if (failOnFailure)
        {
            throw new MojoFailureException(message, cause);
        }
        else
        {
            getLog().warn(message);
        }
    }

    private static Path searchGitRepository(Path current, boolean checkParents)
    {
        if (current == null)
        {
            return null;
        }
        Path gitDir = current.resolve(GIT_FOLDER);
        if (Files.isDirectory(gitDir))
        {
            return gitDir;
        }
        if (checkParents)
        {
            return searchGitRepository(current.getParent(), true);
        }
        return null;
    }

    private static String headToBranch(String headRef, String def)
    {
        if (headRef.startsWith(GIT_REF_PREFIX))
        {
            String target = headRef.substring(GIT_REF_PREFIX.length()).trim();
            String[] parts = target.split("/");
            if (parts.length > 1)
            {
                return parts[parts.length - 1];
            }
        }
        return def;
    }

    private String dereference(Path repo, String ref) throws IOException
    {
        if (ref.startsWith(GIT_REF_PREFIX))
        {
            String target = ref.substring(GIT_REF_PREFIX.length()).trim();
            return dereference(repo, readFile(repo.resolve(target)));
        }
        else
        {
            return ref;
        }
    }

    private static String readFile(Path path) throws IOException
    {
        return new String(Files.readAllBytes(path), UTF_8).trim();
    }
}
