package de.cubeisland.maven.plugins.githead.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * This mojo adds the git head hash as a variable
 *
 * @goal head
 */
public class HeadMojo extends AbstractMojo
{
    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;
    /**
     * @parameter
     */
    public File gitLocation = new File("." + File.separator + ".git");

    private final String HEAD_FILE_NAME = "HEAD";
    private final String HEAD_REF_PREFIX = "ref:";

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        File headFile = new File(this.gitLocation, HEAD_FILE_NAME);

        BufferedReader reader = null;
        String refPath = null;
        try
        {
            reader = new BufferedReader(new FileReader(headFile));

            String line;
            while ((line = reader.readLine()) != null)
            {
                line = line.trim();
                if (line.toLowerCase().startsWith(HEAD_REF_PREFIX))
                {
                    refPath = line.substring(HEAD_REF_PREFIX.length()).trim();
                    break;
                }
            }

        }
        catch (FileNotFoundException e)
        {
            throw new MojoFailureException("The file " + headFile.getPath() + " could not be found!", e);
        }
        catch (IOException e)
        {
            throw new MojoFailureException("Failed to read the " + HEAD_FILE_NAME + " file", e);
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException ignored)
                {}
            }
        }


        if (refPath == null)
        {
            throw new MojoFailureException("The " + HEAD_FILE_NAME + " file did not container a ref");
        }

        reader = null;
        String commitHash = null;
        File refFile = new File(this.gitLocation, refPath);
        try
        {
            reader = new BufferedReader(new FileReader(refFile));

            while ((commitHash = reader.readLine()) != null)
            {
                commitHash = commitHash.trim();
                if (!commitHash.isEmpty())
                {
                    break;
                }
            }
        }
        catch (FileNotFoundException e)
        {
            throw new MojoFailureException("The file " + refFile.getPath() + " could not be found!", e);
        }
        catch (IOException e)
        {
            throw new MojoFailureException("Failed to read the " + HEAD_FILE_NAME + " file", e);
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException ignored)
                {}
            }
        }

        if (commitHash == null)
        {
            throw new MojoFailureException("Could not read the commit hash from " + refFile.getPath() + "!");
        }

        Properties properties = this.project.getProperties();
        properties.put("githead.branch", refFile.getName());
        properties.put("githead.commit", commitHash);
    }
}
