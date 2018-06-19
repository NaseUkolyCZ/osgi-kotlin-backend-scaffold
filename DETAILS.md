# OSGi Starter-Kit
Bare minimum starting point for OSGi based web apps.


## Quick start
1. `git clone` this repo then `gradle syncLibs`;
2. `./new.sh <proj>` to create your own sub project for bundles;
3. Code your sub project as normal *Java/Scala/Kotlin* project, specify `Export-Package: ...` in its `bnd.bnd` meta file; 
4. Build your `/src/*.java, *.scala, *.kt` code in the sub project;
    - Option A: click **`<proj>`** in **IntelliJ IDEA Gradle projects** window (tabs on the right) then click **Execute Gradle Task** (the Gradle icon) type `build` in the prompt.
    - Option B: run `gradle :<proj>:build` in command-line in root project.

Now, you should find a `<proj>.jar` (or jars) under `/runtime/bundle/hot-deploy/subprojects` which will automatically install and start if your OSGi runtime is up. You can change the polling threshold by changing the following configure of the OSGi runtime,
```
# /runtime/conf/config.properties
...

felix.fileinstall.poll=2000
...

```

**Tip**: Repeat the build step when your code changes. This will automatically update the old deployment of the bundle jar in the runtime. 

**Tip**: You can remove the deployed bundle/dep jars from `/runtime/bundle/hot-deploy` to undeploy them. 

**Note**: We did **NOT** use any of the bnd plugins for building the bundle jar, make sure you put bundle manifest directly into the `bnd.bnd` files instead of the `build.gradle` files in the sub projects.

## IDE support (optional)
We support using the [**IntelliJ IDEA CE**](https://www.jetbrains.com/idea/download/) IDE out of the box and treat the sub projects as **Modules**.

The IDE metadata in root project supports the following **IntelliJ IDEA CE** version and above
```
IntelliJ IDEA 2017.1.4
Build #IC-171.4694.23, built on June 6, 2017
```
- osgi-starterkit.iml (Gradle root module metadata)
- osgi-starterkit.ipr (IDE project metadata)
- osgi-starterkit.iws (IDE local, omitted by git)


### 1st time openning in IntelliJ IDEA
1. Double click `osgi-starterkit.ipr` to open IntelliJ IDEA
2. Link as *Gradle project* through the **Unlinked Gradle project?** prompt message. **Uncheck everything** and only **check use local Gradle** and type in the Gradle home path. You can use the following task to find out this path.
```
gradle homeForIDE
```

After this you should see all of the sub projects as **Modules** and the **External Libraries** populated by the local Gradle cache (pulled by the dependencies set in root `build.gradle`).


## OSGi Runtime
Apache Felix Framework [5.6.4](http://felix.apache.org/downloads.cgi)


### Keeping it up
Use `./start.sh` to start the OSGi runtime, this will also start depended library and your custom sub project bundles. This will enter the default **Gogo** command-line for runtime commands, also monitoring hot deployments of new/existing bundles. A **Web Console** is also there to help debugging. (`:8010/console/`, see password in runtime config below)

### Config
See `/runtime/conf/config.properties` for comments. It configs both Framework and the other bundles loaded in the OSGi runtime.

### Bundles
All of the OSGi bundles are put under `/runtime/bundle/`, the ones under `felix` are base runtime bundles loaded according to the `felix.auto.deploy.*` configure upon calling `./start.sh`. Others are loaded by **File Install** felix bundle for runtime hot deployment.


#### Default Felix bundles
`/runtime/bundle/felix`

- Main + Resolver + Framework (`/bin/felix.jar` itself)
- Bundle Repository
- Gogo Runtime
- Gogo Command
- Gogo JLine (replaces Gogo Shell, deps: jline, jansi)

#### Extra Felix bundles
`/runtime/bundle/felix`

- File Install
- Configuration Admin
- Event Admin
- HTTP Servlet 3.0 API
- HTTP Service Jetty (:8010/)
- SCR (Declarative Services)
- Log
- [Web Console](http://felix.apache.org/documentation/subprojects/apache-felix-web-console.html#configuration) (:8010/console/, deps: commons-io, commons-fileupload)

**Note**: The *Web Console* bundles are optional in production environment, it helps debug through logging during development.

#### Library bundles
`/runtime/bundle/hot-deploy/libs`

- Jersey Container servlet (deps: ..17.. jars)
- BND bndlib (deps: slf4j-api, slf4j-simple)
- Scala standard library
- Kotlin OSGi bundle (reflect, std-lib)

Put extra dependency jars into the libs folder if your sub project bundle complains **Unresolved requirements** at runtime. Re-deploy your sub project bundle after adding dependency jars.

#### Application bundles
`/runtime/bundle/hot-deploy/subprojects`

- `<project>`.jar

Use `gradle :<project>:build` to have your sub project bundle built and deployed into the folder.


## Workspace (Custom bundles)
Create sub projects under `/subprojects` (a **bnd workspace**) and build your custom bundles (**bnd projects**) within them. Other than the `/subprojects/cnf` folder any other folder should be considered bnd project as well as a **Module** in the **IntelliJ IDEA** IDE project.

**Tip**: A bnd workspace is just a normal folder with an empty `cnf/build.bnd` meta file and an also empty `cnf/ext` folder. You can use `./bnd.sh add workspace <workspace>` to create them but we already created one named `subprojects` for you and pre-configured with a Maven bnd repo. 

Create a new sub project with **bnd** meta file, **Gradle** build script and **IntelliJ IDEA** module meta file by
```
./new.sh <proj>
```


### Dependencies (3rd party jar)
Gradle build tool has our dependencies configured in the root `build.gradle` file. The dependencies configure is shared among the sub projects by default. We also put copies of theses libraries into `/subprojects/cnf/libs`.

You can use the `syncLibs` task to refresh the dependencies after changing it in the root `build.gradle` file
```
gradle syncLibs
```

Note that `/subprojects/cnf/libs` is different than the `/runtime/bundle/hot-deploy/libs` folder which is for OSGi runtime. `/subprojects/cnf/libs` is for examining the dependency jars before picking into the deployment library folder. It is only there for you to look at and copy into runtime. It does not concern with building your bundles in the sub projects either. **You can also use these copies for re-bundling purpose through bnd**

**Tip**: Shared dependency is better for our bnd workspace setup since all of the sub projects dependency (for compiling) can be controlled in a centralized way. It helps align the deployed library bundles in the final OSGi runtime as well.

You can use different versions of the same artifact (a jar in Maven terms) in these projects for both compile and OSGi runtime. This is honored by the bnd build tool. Use `Import-Package` header in your `bnd.bnd` project bundle meta file to control versioning of the depended packages at runtime.

**Tip**: By default, bnd uses `Import-Package: *` for a bundle's default value for package dependency resolving in the OSGi runtime. If you have multiple versions of the same package as requirements in different bundles you can put into your `bnd.bnd` meta like this, just don't forget to put `*` in the end (or else your bundle will resolve but might be unable to activate due to lack of other imported packages indicated by `*`).
```
# <proj A> bnd.bnd

Import-Package: \
    org.name.util;version="[1.0,..)", \
    ...
    *

# <proj B> bnd.bnd
Import-Package: \
    org.name.util;bundle-version="[2.0,..)", \
    ...
    *
```
Note that `;version` resolves to package versioning while `;bundle-version` resolves to bundle (jar) versioning.

### Alternative deps management (not in use)
The bnd build tool supports pulling dependency jars from Maven Central through its [**Maven Bnd Repository Plugin**](http://bnd.bndtools.org/plugins/maven.html) into our pre-configured bnd repo `~/.m2/repository/` (the default Maven repo on your system) using Maven coordinates set in the `/subprojects/cnf/ext/deps.maven` index file. However this does *NOT* support pulling transitive dependency jars in addition to those specified in the index file. This is by design. Since the bnd build process is not there to help compile the Java code. It only concerns pulling packages from bytecode and pack with generated `MANIFEST.MF` OSGi metadata into jars. So, be ready to add all the transitive Maven coordinates for your library manually into `deps.maven` if you want to control your compiling time dependencies (a.k.a libraries) this way.

**Note:** Unlike the built-in bnd plugins, the **Maven Bnd Repository Plugin** is not enabled by default, we used the following cmd to explicitly enable it
```
./bnd.sh add plugin MavenBndRepository
```
After this, `/subprojects/cnf` will have a `MavenBndRepository.bnd` configure file for the enabled plugin. We have pre-configured it to pull everying specified in `deps.maven` into `~/.m2/repository/`. You can verify pulled Maven bundles by using
```
./bnd.sh repo list
```
Note that, although this repo path seems to overlap with your system's default Maven repo. Only jars listed by `deps.maven` is made available in this bnd workspace.

**Tip**: Add transitive dependencies (in coordinate format) into the `deps.maven` file as well if you want bnd to pull all dependency jars from Maven Central.

Even though we can pull dependencies through bnd, we will need to add the `~/.m2/repository/` folder as a *Library* to the **IntelliJ IDEA** IDE project through **Project Structure** --> **Libraries** --> **+** --> **Java** --> select folder `~/.m2/repository/` with type **Jar Directory**. This is to support the auto-import and code-completion in the IDE when writing Java code, also for compiling the Java code.

The only perk of using bnd to pull dependency jars from Maven is for convenient bundle packing through `-buildpath` instruction in your `bnd.bnd` project bundle meta file. It offers the fastest way of bundling 3rd party packages (from these jars) into your own bundle. This perk is quickly outweighted by the fact that these dependency jars (Maven coordinates) have to be explicitly added into `deps.maven` index and even without this Maven bnd repo we can still use the `-classpath` instruction in `bnd.bnd` to achieve the same result with the help of a bnd macro. Also most of us get confused by the fact that these jars are not there for compiling our Java code, instead, they are for packing the final bundle jar. This is why bnd don't pull transitive jars for you. The bnd project doesn't concern compiling the Java code, only bundling jars and it doens't want you to pull every compiling dependency packages into your final bundle (as a repeatitive ball of mud they say...), those packages should live within their own bundle and shared within the OSGi runtime.

**Tip**: By our default workspace setup, you don't need to worry about using bnd to pull project dependencies. It is there, but only worth using if the whole purpose of your workspace is to combine 3rd party bundles in sub projects.

### Scaffolding a new bundle project
run `./new.sh <name>` to create a new bnd project to produce OSGi bundles (e.g usually an interface Contract APIs jar and an implementation Provider jar). 

### Build
Build your code by *compiling* it with dependencies. Build your jar by *bundling* it with bnd generated `MAIFEST.MF` file and bnd extracted packages (and resources) from the compiled bytecode. It is important to know that, by default, the bnd build tool doesn't concern compiling Java code, it only pulls compiled packages out of the bnd sub project's `/bin` folder, the 3rd party jars (through `-classpath:` in `bnd.bnd`) and classes from jars in bnd repos (through `-buildpath:` in `bnd.bnd`) and put them into the final bundle jar.

**Principal 1**: Remember, whoever compiles the code, knows the dependencies (both within a sub project and across the workspace). This is achievable by either using the **IntelliJ IDEA** IDE or, for headless builds, the **SBT** build tool or the not so new **Gradle** tool or even the good old **Maven** tool.

**Principal 2**: Whoever bundles the jar, doesn't care dependencies other than those put on classpath (e.g in case of bnd, by project `/bin` and `-buildpath/-classpath` instruction). The `Import/Export-Package` meta will only be consulted by OSGi runtime. 

Both processes can use Maven Central, however, they are different processes in nature. If you are wondering how we could *smartly* combine these two process with just a single control point (like an Maven index that looks like `packages.json` in NPM or `requirement.txt` in pip) then we are with you. The extra tooling you will be looking for is the available bnd plugins in Gradle or Maven. Still, your ignorance will be punished by OSGi and bnd because they *hate* inexplicit-ness (e.g transitive dependencies). None of the work mentioned saves the trouble to maintain your `*.bnd` meta file content (well, specifying same thing in *Groovy* or *XML* isn't going to cut it, is it?). 

In short, (after you have sorted out the `bnd.bnd` meta file) here is the sub `<proj>` build process:

1. Execute the `gradle :<proj>:build` task on your sub project either from the IDE or the command-line.

That's it! The built bundle (bundles, if you have more than 1 `*.bnd` file) will be put into `/runtime/bundle/hot-deploy/subprojects` folder and be picked up by the *File Install* felix bundle for auto install and start.


#### Compile it
It is Java, so you need to compile the code into bytecode before it can be packed by bnd into bundle jars. There are two ways to compile your sub project code--IDE and headless.

Note that normally, you should **NOT** concern yourself with the compile phase in the build process.


##### Compile through IDE
Since we have setup our bnd workplace sub projects as **IntelliJ IDEA** IDE project **Module**s throught the **idea Gradle plugin**, it is very easy to compile the code, just click **Build Module `<proj>`** in the right-clicking menu on your **Module**.

##### Compile headlessly
Compile a sub project with Gradle
```
gradle :<proj>:classes
```

Build a sub project (including compiling and jaring)
```
gradle :<proj>:build
```

To make Gradle use a local folder of jars (e.g `subprojects/<proj>/myLibs/*.jar`) as dependency, add the following into the sub project's `build.gradle`
```
dependencies {
    compile fileTree(dir: 'myLibs', include: '*.jar')
}
```

Note that we have pre-configured our Gradle tasks and IntelliJ IDEA Module to use the default bnd project layout. Your build and compile tasks will output classes into `bin` and `bin_test` folders and output jars and intermediate files into `generated` folder within any sub project.


#### Bundle it
Notice that we didn't use any IDE feature to pack the sub project jars (a.k.a OSGi bundles). Instead, we will use bnd in command line. The **IntelliJ IDEA** IDE offers a nice **Terminal** tab for you to keep focusing within the workspace and use the system command line for bnd jar packing. 

You will find that developing your bundle sub project is relatively straightforward with dependency jars pulled directly as libraries from Maven Central up till code compilation, afterwards, it comes to actually building the bundle (basically a normal jar with `MANIFEST.MF` file specifying packages visibilites and versioning) we will need bnd to help generate the `MANIFEST.MF` and pack it with compiled Java bytecode into a jar. You can think of *build* in bnd as OSGi metadata generation (the label) plus bytecode Java packages (the goods) extraction. 

The bnd build tool doesn't know which packages (again, the whole OSGi and bnd tooling thing is about Java packages) to include in the jar except for those that comes from sub project `/bin`. To let bnd work extra, in each of the sub project we have a `bnd.bnd` file to specify `-buildpath`/`-classpath`, `-includeresource` bnd instructions and `Export-Package`/`Private-Package`, `Bundle-Version` bnd headers. These are for collecting packages from various other targets (bnd repos or *.jar) and expose selected packages in the final OSGi bundle. You will most likely use `Export-Package` at least in your `bnd.bnd` project bundle meta file.

**Tip**: As we have mentioned, the bundling process doesn't concern compile time dependencies. However, there are times when you do need to bundle some packages that come from the 3rd party jars. With `-buildpath` specified packages in `bnd.bnd` the bnd tool only searches bnd repos to include those. If you also want it to include packages from a 3rd party jar folder, use `-classpath` with bnd macro `${lsr;${workspace}/cnf/libs;*.jar}` to add them. If the 3rd party package somehow doesn't like your final bundle folder structure, use `-includeresource` instead with the `@jar/cnf/libs/<any>.jar` unpack helper to add the packages with original folder structures merged in your final jar. The known fastest way of repacking 3rd party bundles into yours is through the `-includeresource` bnd instruction in the `bnd.bnd` meta file in a sub project.


### Deploy
Put your bundle jar into `/runtime/bundle/hot-deploy/` to deploy, if your OSGi runtime is running, this should install/update the bundle and starts it.

Still, you have 2 options in deploying (more like 2 options for bundling your jar).


#### Option A: Thin App Bundle
By default, we encourage using a light `bnd.bnd` meta (without `-classpath` and `-includereource`) for a thin application bundling scheme. This way, your sub project bundle will only contain business logic rather than the api libraries.

**Important**: At this stage, if your OSGi runtime complains, you might need to move some of the jars from `/subprojects/cnf/libs` into `/runtime/bundle/hot-deploy/libs` so the required extra packages (Class or Interface bytecode) are made available in the runtime. This might looks strange if you wonder why we need two separate libs folder to compile and run the sub project bundle jar respectively. This is by design. Short answer is we want to keep code compilation separate from bundle runtime. The APIs (Interface) your project compiled against might be fulfilled by some other Providers (Class) in the runtime. The OSGi runtime is there to share packages (Class and Interface) and service components (Instances).

#### Option B: Fat All-in-One Bundle
You can also choose to use `-includeresource` (or `-classpath`) instruction in `bnd.bnd` to merge 3rd party bundle content with your sub project bundle. This way, all of the packages and resources from your dependencies will be packed into your final jar. You still pack your jar through bnd but this time you don't need to worry about moving `/subprojects/cnf/libs` jars into `runtime/bundle/hot-deploy/libs` since the packages requried are already local to your bundle.

**Note**: It is only worth using **Option B** if there are not a lot of transitive jars to merge and only you are using some of the specific packages that other sub project are not interested in.


## Run
Call `./start.sh` to run the entire OSGi stack, note that if you include the Gogo jars (3 of them), you will be prompt with `g!` and entering the Gogo command shell after the stack is started. Also, if you include the Web Console jar, there will be a web console deployed for you to inspect the OSGi runtime bundles, status and logs. The logs are pretty useful for debugging your bundle exceptions.

Check within `./start.sh` to see how you can load a different set of bundles to begin with (e.g through a different configure file for production).


## Demo bundle
`:8010/example/status` (example.jar) using Jersey 2.25+.


## Useful links
- [OSGi Specs v6](https://www.osgi.org/developer/specifications/)
- [Apache Felix v5](http://felix.apache.org/documentation.html)
- [Apache Felix v5 sub projects](http://felix.apache.org/documentation/subprojects.html)
- [OSGi enRoute](http://enroute.osgi.org/)
- [Bndtools bnd](http://bnd.bndtools.org/chapters/150-build.html)
(
[7](http://bnd.bndtools.org/chapters/140-best-practices.html),
[4](http://bnd.bndtools.org/chapters/123-tour-workspace.html),
[23](http://bnd.bndtools.org/chapters/610-plugin.html),
[5](http://bnd.bndtools.org/chapters/125-tour-features.html),
[20](http://bnd.bndtools.org/chapters/390-wrapping.html),
[26](http://bnd.bndtools.org/chapters/800-headers.html),
[28](http://bnd.bndtools.org/chapters/825-instructions-ref.html),
[29](http://bnd.bndtools.org/chapters/850-macros.html)
)
- [Gradle build](https://gradle.org/guides/#getting-started)
- [Maven Repository](https://mvnrepository.com/)
- [IntelliJ .gitignore](https://www.gitignore.io/api/intellij)


## License
Copyright 2017 Tim Lauv. Copyright 2018 David Podhola. 
Under the [MIT](http://opensource.org/licenses/MIT) License.
