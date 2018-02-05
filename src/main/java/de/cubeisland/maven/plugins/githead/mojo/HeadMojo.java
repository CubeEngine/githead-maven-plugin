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
package de.cubeisland.maven.plugins.githead.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * This mojo adds the git head hash as a variable
 *
 * @goal head
 * @phase initialize
 * @threadSafe
 */
public class HeadMojo extends AbstractMojo
{
    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project = null;

    /**
     * @parameter default-value="${project.basedir}"
     */
    protected File repoLocation = null;

    /**
     * @parameter
     */
    protected boolean searchParentDirectories = true;

    /**
     * @parameter
     */
    protected String defaultBranch = "unknown";

    /**
     * @parameter
     */
    protected String defaultCommit = this.defaultBranch;

    private static final String HEAD_FILE_NAME = "HEAD";
    private static final String HEAD_REF_PREFIX = "ref:";
    private static final String GIT_FOLDER = ".git";

    private static final Charset CHARSET = Charset.forName("UTF-8");

    public void execute() throws MojoExecutionException
    {
        File repo = this.repoLocation.getAbsoluteFile();
        try
        {
            repo = repo.getCanonicalFile();
        }
        catch (IOException e)
        {
            getLog().debug("Failed to canonicalize the repo path!", e);
        }
        getLog().info("Default location: " + repo.getAbsolutePath());
        File tmp;
        do
        {
            getLog().info("Searching in: " + repo.getAbsolutePath());
            tmp = new File(repo, GIT_FOLDER);
            if (tmp.exists() && tmp.isDirectory())
            {
                getLog().info("Found at: " + repo.getAbsolutePath());
                break;
            }
        }
        while (this.searchParentDirectories && (repo = repo.getParentFile()) != null);

        String branch = null;
        String commit = null;

        if (repo != null && repo.exists() && repo.isDirectory())
        {
            final String head = this.readHead(repo);
            if (head != null)
            {
                getLog().info("HEAD: " + head);
                if (head.startsWith(HEAD_REF_PREFIX))
                {
                    final String ref = head.substring(HEAD_REF_PREFIX.length()).trim();
                    branch = refToBranch(ref);
                    commit = readHeadCommit(repo, ref);
                }
                else
                {
                    commit = head.trim();
                }
            }
        }
        else
        {
            getLog().warn("No valid git repository found! Started from: " + this.repoLocation.getAbsolutePath());
        }

        if (branch == null)
        {
            branch = this.defaultBranch;
        }
        if (commit == null)
        {
            commit = this.defaultCommit;
        }

        Properties properties = this.project.getProperties();
        properties.put("githead.branch", branch);
        properties.put("githead.commit", commit);

        getLog().info("Found: " + branch + "/" + commit);
    }

    private static String refToBranch(String ref)
    {
        String[] parts = ref.split("/");
        String branch = parts[parts.length - 1].trim();
        if ("".equals(branch))
        {
            return null;
        }
        return branch;
    }

    private String readHeadCommit(File repo, String ref)
    {
        File refFile = new File(repo, GIT_FOLDER + File.separator + ref);

        BufferedReader reader = null;
        String commitHash = null;
        try
        {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(refFile), CHARSET));

            while ((commitHash = reader.readLine()) != null)
            {
                commitHash = commitHash.trim();
                if (!"".equals(commitHash))
                {
                    break;
                }
            }
        }
        catch (FileNotFoundException e)
        {
            getLog().warn("The file " + refFile.getPath() + " could not be found!", e);
        }
        catch (IOException e)
        {
            getLog().warn("Failed to read the ref file", e);
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException e)
                {
                    getLog().debug("Failed to close the ref file!", e);
                }
            }
        }
        return commitHash;
    }

    private String readHead(File repo)
    {
        File headFile = new File(repo, GIT_FOLDER + File.separator + HEAD_FILE_NAME);

        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(headFile), CHARSET));

            String line = reader.readLine();
            if (line != null)
            {
                return line.trim();
            }
        }
        catch (FileNotFoundException e)
        {
            getLog().warn("The file " + headFile.getPath() + " could not be found!", e);
        }
        catch (IOException e)
        {
            getLog().warn("Failed to read the " + HEAD_FILE_NAME + " file", e);
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException e)
                {
                    getLog().debug("Failed to close the HEAD file!", e);
                }
            }
        }
        return null;
    }
}
