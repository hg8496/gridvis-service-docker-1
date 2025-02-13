package utils

import de.janitza.pasw.project.api.state.BaseProjectState
import de.janitza.pasw.project.api.state.IProjectStateHandler

import java.util.logging.Logger

import de.janitza.pasw.database.ConfigurationItems
import de.janitza.pasw.database.IDatabaseManager
import de.janitza.pasw.database.migration.IMigration
import de.janitza.pasw.database.tools.MigrationHelper
import de.janitza.pasw.energy.api.IProjectList
import de.janitza.pasw.energy.common.ServerLkp
import de.janitza.pasw.project.api.IProjectListProvider
import de.janitza.pasw.project.base.DatabaseProjectInitializer
import de.janitza.pasw.project.server.IProjectManager
import org.openide.util.Lookup

class ProjectTools {

    private static final Logger LOGGER = Logger.getLogger(ProjectTools.class.getName())
    private static final PROJECT_BASE_DIR = "/opt/GridVisProjects"

    static getProjects() {
        return ServerLkp.lookup(IProjectList.class).getAllReadyProjects()
    }

    static String getProjectPath() {
        def prjName = System.getenv('PROJECT_NAME') ?: 'default'
        return new File(PROJECT_BASE_DIR, prjName).getAbsolutePath()
    }

    static boolean checkProjectDir() {
        new File(getProjectPath()).exists()
    }

    static createProject(Map<ConfigurationItems, String> configMap, String dbType) {
        def prjFile = new File(getProjectPath())
        def initializer = new DatabaseProjectInitializer(prjFile, dbType, configMap)
        initializer.createDirecotry()
        new File(prjFile, "symbols").mkdirs()
        initializer.createConfigFile()
        initializer.createDatabase()
        def manager = Lookup.getDefault().lookup(IProjectManager.class)
        manager.openProject(prjFile.getAbsolutePath())
    }

    static createJanDBProject() {
        def configMap = Collections.singletonMap(ConfigurationItems.ProjectDir, getProjectPath())
        createProject(configMap, "janitzadb")
    }

    static createMySQLProject() {
        def configMap = new HashMap<ConfigurationItems, String>()
        configMap.put(ConfigurationItems.Host, System.getenv('DB_HOST') ?: 'database')
        configMap.put(ConfigurationItems.Port, System.getenv('DB_PORT') ?: '3306')
        configMap.put(ConfigurationItems.User, System.getenv('DB_USER') ?: 'root')
        configMap.put(ConfigurationItems.Password, System.getenv('DB_PASSWORD') ?: 'rootPWD')
        configMap.put(ConfigurationItems.Database, System.getenv('DB_DATABASE') ?: 'GridVis')
        createProject(configMap, "mysql")
    }

    static createMSSQLProject() {
        def configMap = new HashMap<ConfigurationItems, String>()
        configMap.put(ConfigurationItems.Host, System.getenv('DB_HOST') ?: 'database')
        configMap.put(ConfigurationItems.Port, System.getenv('DB_PORT') ?: '1433')
        configMap.put(ConfigurationItems.User, System.getenv('DB_USER') ?: 'sa')
        configMap.put(ConfigurationItems.Password, System.getenv('DB_PASSWORD') ?: 'saPWD')
        configMap.put(ConfigurationItems.Database, System.getenv('DB_DATABASE') ?: 'GridVis')
        createProject(configMap, "mssqlserver")
    }

    static checkMigration() {
        Lookup.getDefault().lookup(IProjectListProvider.class).getAllProjects().each { p ->
            if(p.getLookup().lookup(IProjectStateHandler.class).getState() != BaseProjectState.Ready) {
                final IDatabaseManager ret = p.getLookup().lookup(IDatabaseManager.class)
                final Collection<? extends IMigration> migrations = ret.migrations
                if (migrations.size() > 0) {
                    println "Performing database migration"
                    MigrationHelper.doMigrations(migrations)
                }
            }
        }
    }

    static getProject() {
        def projects = Lookup.getDefault().lookup(IProjectListProvider.class).allReadyProjects
        return projects.size() > 0 ? projects.get(0) : null
    }

    static minuteJob() {
        System.out.println("Minute Job")
        checkMigration()
    }
}
