Building the toolkit
====================
Note: Ensure the following software is installed and configured correctly:
    - Apache Ant version 1.7.1 or later.
    - Java Wireless Toolkit version 2.5.2 or later.

1. To build the classes and tools:
        ant all
   This will build the toolkit for all supported Java platforms, along with any additional tools.

3. The following output will be created:
        - classes/j2me - Directory containing the compiled classes for J2ME.
        - tools/j2me/* - The application binaries for each tool.

        
Using the toolkit
=================
To use the toolkit as part of a client project, perform the following steps:
    1. Copy the "lib\svnant.jar" and "lib\svnClientAdapter.jar" files to the "lib" directory of your project. These will 
    be needed to retrieve the toolkit from SVN at build time. You will also need to ensure that a command-line "svn" 
    client (e.g. SlikSVN) is available on the PATH.

    2. Update your build file (usually "build.xml") to retrieve the appropriate version of the toolkit from SVN. For
       example:
            <!-- Get the Mobile Toolkit from the SVN server (requires the Svnant libraries in the "lib" directory) --> 
            <property name="mtk.dir" location="common/mtk"/>
            <property name="mtk.url" value="http://gforge.cpth.ie/svn/mobiletools/Java/trunk"/>
            <echo message="Getting Mobile Toolkit from '${mtk.url}' to '${mtk.dir}'..."/>
            <delete dir="${mtk.dir}" quiet="true"/>
            <typedef resource="org/tigris/subversion/svnant/svnantlib.xml" classpath="${lib.dir}/svnant.jar:${lib.dir}/svnClientAdapter.jar"/>
            <svn username="${svn.username}" password="${svn.password}" javahl="false" failonerror="true">
                <export srcUrl="${mtk.url}" destPath="${mtk.dir}"/>
            </svn>
    
    3. Import the toolkit build tools to get access so some useful macros:
            <import file="${mtk.dir}/build/buildtools.xml"/>
       You will then be able to use the "<mtk.xxx>" macros in your own build files (see "buildtools.xml" for details of 
       the available macros and properties).
       Note: this import must be placed at the top-level in your build file. This means that retrieving the toolkit from 
       SVN must also be at the top-level. One consequence of this is that you cannot use any Ant tasks that re-evaluate 
       the build file like "<antcall>" (use "<macrodef>" instead).
       
   4. Include the toolkit source when actually building your own client. This is usually done via pre-processing so that 
      any pre-processing symbols in the toolkit (see "mtk.symbols") are correctly processed:
            <!-- Pre-process the Mobile Toolkit source -->
            <mtk.preprocessMtkSource destdir="${src.processed.dir}" platform="j2me" device="Generic/Java"/>
      If you want to perform your own preprocessing, you can first obtain the toolkit source for a particular platform 
      as follows:
            <mtk.copyMtkSource destDir="${src.preprocessed.dir}" platform="j2me"/>
