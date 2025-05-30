package org.rundeck.tests.functional.api.project

import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.responses.common.ConfigProperty
import org.rundeck.util.api.responses.nodes.Node
import org.rundeck.util.api.responses.project.ProjectCreateResponse
import org.rundeck.util.api.responses.project.ProjectSource
import org.rundeck.util.api.responses.system.SystemInfo
import org.rundeck.util.container.BaseContainer
import com.fasterxml.jackson.core.type.TypeReference
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml

@APITest
class ConfigSpec extends BaseContainer{

    def "test-project-config-key-json"(){
        given:
        def client = getClient()
        def projectName = "testConfigJson"
        Object testProperties = [
                "name": projectName,
                "config": [
                        "test.property": "test value",
                        "test.property2": "test value2"
                ]
        ]

        when:
        def response = client.doPost(
                "/projects",
                testProperties
        )
        assert response.successful
        ProjectCreateResponse parsedResponse = MAPPER.readValue(
                response.body().string(),
                ProjectCreateResponse.class
        )

        then:
        parsedResponse.name != null
        parsedResponse.name == projectName

        parsedResponse.config."test.property" == "test value"
        parsedResponse.config."test.property2" == "test value2"
        !parsedResponse.config."test.property3"

        when: "TEST: GET config"
        def responseForProp1 = doGet("/project/${projectName}/config/test.property")
        assert responseForProp1.successful
        ConfigProperty prop1 = MAPPER.readValue(responseForProp1.body().string(), ConfigProperty.class)

        def responseForProp2 = doGet("/project/${projectName}/config/test.property2")
        assert responseForProp2.successful
        ConfigProperty prop2 = MAPPER.readValue(responseForProp2.body().string(), ConfigProperty.class)

        then:
        prop1.key == "test.property"
        prop1.value == "test value"

        prop2.key == "test.property2"
        prop2.value == "test value2"

        when: "TEST: PUT (update) config"
        def updatedValueForProp1 = "A better value"
        def updatedBodyForProp1 = [ "key":"test.property", "value":updatedValueForProp1 ]

        def updatedValueForProp2 = "A better value2"
        def updatedBodyForProp2 = [ "key":"test.property2", "value":updatedValueForProp2 ]

        def updated1 = client.doPutWithJsonBody("/project/${projectName}/config/test.property", updatedBodyForProp1)
        assert updated1.successful

        def updated2 = client.doPutWithJsonBody("/project/${projectName}/config/test.property2", updatedBodyForProp2)
        assert updated2.successful

        def responseForUpdatedProp1 = doGet("/project/${projectName}/config/test.property")
        assert responseForUpdatedProp1.successful

        def responseForUpdatedProp2 = doGet("/project/${projectName}/config/test.property2")
        assert responseForUpdatedProp1.successful


        ConfigProperty updatedProp1 = MAPPER.readValue(responseForUpdatedProp1.body().string(), ConfigProperty.class)
        ConfigProperty updatedProp2 = MAPPER.readValue(responseForUpdatedProp2.body().string(), ConfigProperty.class)

        then:
        updatedProp1.value != "test value"
        updatedValueForProp1 == updatedProp1.value

        updatedProp2.value != "test value2"
        updatedValueForProp2 == updatedProp2.value

        when: "create a new prop"
        def updatedValueForProp3 = "A better value3"
        def updatedBodyForProp3 = [ "key":"test.property3", "value":updatedValueForProp3 ]

        def updated3 = client.doPutWithJsonBody("/project/${projectName}/config/test.property3", updatedBodyForProp3)
        assert updated3.successful

        def responseForUpdatedProp3 = doGet("/project/${projectName}/config/test.property3")
        assert responseForUpdatedProp3.successful

        ConfigProperty updatedProp3 = MAPPER.readValue(responseForUpdatedProp3.body().string(), ConfigProperty.class)

        then:
        updatedProp3 != null
        updatedValueForProp3 == updatedProp3.value

        when: "All config updated"
        def allConfigResponse = doGet("/project/${projectName}/config")
        assert allConfigResponse.successful
        Map<String, Object> props = MAPPER.readValue(allConfigResponse.body().string(), HashMap<String, Object>.class)

        then:
        props != null
        props."test.property" == updatedValueForProp1
        props."test.property2" == updatedValueForProp2
        props."test.property3" == updatedValueForProp3

        cleanup:
        deleteProject(projectName)
    }

    def "test-project-config"(){
        given:
        def client = getClient()
        def projectName = "testConfig"
        Object testProperties = [
                "name": projectName,
                "config": [
                        "test.property": "test value",
                        "test.property2": "test value2"
                ]
        ]

        when:
        def response = client.doPost(
                "/projects",
                testProperties
        )
        assert response.successful
        ProjectCreateResponse parsedResponse = MAPPER.readValue(
                response.body().string(),
                ProjectCreateResponse.class
        )

        then:
        parsedResponse.name != null
        parsedResponse.name == projectName

        parsedResponse.config."test.property" == "test value"
        parsedResponse.config."test.property2" == "test value2"

        when: "TEST: GET config"
        def responseForProp1 = doGet("/project/${projectName}/config/test.property")
        assert responseForProp1.successful
        ConfigProperty prop1 = MAPPER.readValue(responseForProp1.body().string(), ConfigProperty.class)

        def responseForProp2 = doGet("/project/${projectName}/config/test.property2")
        assert responseForProp2.successful
        ConfigProperty prop2 = MAPPER.readValue(responseForProp2.body().string(), ConfigProperty.class)

        then:
        prop1.key == "test.property"
        prop1.value == "test value"

        prop2.key == "test.property2"
        prop2.value == "test value2"

        when: "bulk update"
        def updatedProps = [
                "test.property":"updated value 1",
                "test.property3":"created value 3"
        ]
        def updatedResponse = client.doPutWithJsonBody("/project/${projectName}/config", updatedProps)
        Map<String, Object> parsedUpdatedProps = MAPPER.readValue(updatedResponse.body().string(), HashMap<String, Object>.class)

        then:
        parsedUpdatedProps."test.property" == "updated value 1"
        parsedUpdatedProps."test.property3" == "created value 3"

        cleanup:
        deleteProject(projectName)
    }

    def "test-project-create-json"(){
        given:
        def client = getClient()
        def projectName = "testProjectCreate"
        def projectDescription = "a description"
        Object testProperties = [
                "name": projectName,
                "description": projectDescription,
                "config": [
                        "test.property": "test value",
                        "test.property2": "test value2"
                ]
        ]

        when:
        def response = client.doPost(
                "/projects",
                testProperties
        )
        assert response.successful
        ProjectCreateResponse parsedResponse = MAPPER.readValue(
                response.body().string(),
                ProjectCreateResponse.class
        )

        then:
        parsedResponse.name != null
        parsedResponse.name == projectName

        parsedResponse.config."test.property" == "test value"
        parsedResponse.config."test.property2" == "test value2"

        parsedResponse.description == projectDescription

        cleanup:
        deleteProject(projectName)
    }

    def "test-project-create"(){
        given:
        def client = getClient()
        def projectName = "testProjectCreateConflict"
        def projectDescription = "a description"
        Object testProperties = [
                "name": projectName,
                "description": projectDescription,
                "config": [
                        "test.property": "test value",
                ]
        ]

        when: "TEST: POST /api/14/projects"
        def response = client.doPost(
                "/projects",
                testProperties
        )
        assert response.successful
        ProjectCreateResponse parsedResponse = MAPPER.readValue(
                response.body().string(),
                ProjectCreateResponse.class
        )

        then:
        parsedResponse.name != null
        parsedResponse.name == projectName

        parsedResponse.config."test.property" == "test value"

        when: "TEST: POST /api/14/projects (existing project results in conflict)"
        def conflictedResponse = client.doPost(
                "/projects",
                testProperties
        )

        then:
        !conflictedResponse.successful
        conflictedResponse.code() == 409

        cleanup:
        deleteProject(projectName)
    }

    def "test-project-delete-v45"(){
        given:
        def client = getClient()
        def projectName = "testProjectDeleteDeferred"
        def projectDescription = "a description"
        Object testProperties = [
                "name": projectName,
                "description": projectDescription,
                "config": [
                        "test.property": "test value",
                ]
        ]

        when: "TEST: POST /api/14/projects"
        def response = client.doPost(
                "/projects",
                testProperties
        )
        assert response.successful
        ProjectCreateResponse parsedResponse = MAPPER.readValue(
                response.body().string(),
                ProjectCreateResponse.class
        )

        then:
        parsedResponse.name != null
        parsedResponse.name == projectName

        when: "#TEST: delete project"
        def deleteResponse = doDelete("/project/${projectName}?deferred=false")
        assert deleteResponse.successful

        then:
        deleteResponse.code() == 204
    }

    def "test-project-delete"(){
        given:
        def client = getClient()
        def projectName = "testProjectDelete"
        def projectDescription = "a description"
        Object testProperties = [
                "name": projectName,
                "description": projectDescription,
                "config": [
                        "test.property": "test value",
                ]
        ]

        when: "TEST: POST /api/14/projects"
        def response = client.doPost(
                "/projects",
                testProperties
        )
        assert response.successful
        ProjectCreateResponse parsedResponse = MAPPER.readValue(
                response.body().string(),
                ProjectCreateResponse.class
        )

        then:
        parsedResponse.name != null
        parsedResponse.name == projectName

        when: "#TEST: delete project"
        def deleteResponse = doDelete("/project/${projectName}")
        assert deleteResponse.successful

        then:
        deleteResponse.code() == 204
    }

    def "test-project-invalid"(){
        given:
        def client = getClient()
        def projectName = "RhetoricalMiscalculationElephant"

        when:
        def response = client.doGet("/project/$projectName")
        def parsedBody = MAPPER.readValue(response.body().string(), Object.class)

        then:
        response.code() == 404
        parsedBody.errorCode == "api.error.project.missing"
        parsedBody.message == "Project does not exist: $projectName"

    }

    def "test-project-json"(){
        given:
        def client = getClient()
        def projectName = "RhetoricalMiscalculationElephant"

        when: "We check only the format of the response, regardless of the content"
        def jsonResponseBody = client.doGet("/project/$projectName")
        def responseString = jsonResponseBody.body().string()
        def validJsonParse = MAPPER.readValue(responseString, Object.class)

        then:
        !isYamlValid(responseString)
        validJsonParse.errorCode == "api.error.project.missing"
        validJsonParse.error
        validJsonParse.message == "Project does not exist: $projectName"
    }

    def "test-project-missing"(){
        given:
        def client = getClient()
        def projectName = "RhetoricalMiscalculationElephant"

        when:
        def jsonResponseBody = client.doGet("/project/$projectName/resources")

        then:
        !jsonResponseBody.successful
        jsonResponseBody.code() == 404

    }

    def "test-project-resources-404"(){
        given:
        def client = getClient()

        when:
        def jsonResponseBody = client.doGet("/project/someProject/resources")
        def validJsonParse = MAPPER.readValue(jsonResponseBody.body().string(), Object.class)

        then:
        !jsonResponseBody.successful
        jsonResponseBody.code() == 404
        validJsonParse.errorCode == "api.error.project.missing"
        validJsonParse.error
        validJsonParse.message == "Project does not exist: someProject"
    }

    def "test-project-space-in-name-fails"(){
        given:
        def client = getClient()
        def projectName = "test project"
        Object testProperties = [
                "name": projectName,
                "description":"project name with spaces",
                "config": [
                        "test.property": "test value",
                        "test.property2": "test value2"
                ]
        ]

        when:
        def response = client.doPost(
                "/projects",
                testProperties
        )

        then:
        !response.successful
        response.code() == 400
    }

    def "test-project-resources"(){
        given:
        setupProject()
        def client = getClient()

        when: "YAML"
        def yamlResponse = client.doGet("/project/$PROJECT_NAME/resources?format=yaml")
        def responseBody = yamlResponse.body().string()
        MAPPER.readValue(responseBody, Object.class)

        then: "Invalid JSON"
        thrown Exception

        when: "Valid YAML"
        def yaml = new Yaml().load(responseBody)

        then:
        noExceptionThrown()
        yaml != null

        when:
        def jsonResponse = client.doGet("/project/$PROJECT_NAME/resources?format=json")
        def responseJsonBody = jsonResponse.body().string()
        def json = MAPPER.readValue(responseJsonBody, Object.class)

        then: "JSON is invalid"
        !isYamlValid(responseJsonBody)
        json != null

        when: "A unsupported format"
        def unsupportedResponse = client.doGetAcceptAll("/project/$PROJECT_NAME/resources?format=unsupported")
        def unsupportedParsedResponse = MAPPER.readValue(unsupportedResponse.body().string(), Object.class)

        then: "unsupported"
        unsupportedParsedResponse.errorCode == "api.error.resource.format.unsupported"
        unsupportedParsedResponse.error
        unsupportedParsedResponse.message == "The format specified is unsupported: unsupported"

        when:
        client.apiVersion = 2 // as the original test states
        def unsupportedApiVersionResponse = client.doGetAcceptAll("/project/$PROJECT_NAME/resources")
        def parsedUnsupportedApiVersionResponse = MAPPER.readValue(unsupportedApiVersionResponse.body().string(), Object.class)

        then:
        parsedUnsupportedApiVersionResponse.errorCode == "api.error.api-version.unsupported"
        parsedUnsupportedApiVersionResponse.error
        parsedUnsupportedApiVersionResponse.message?.contains("Unsupported API Version \"2\"")

        cleanup:
            client.apiVersion = client.API_CURRENT_VERSION
    }

    def "test-require-versions"(){
        given:
        def client = getClient()
        String apiVersion = testApiVersion
        client.apiVersion

        when: "Api version FAKE"
        def response = client.doGetCustomApiVersion("/projects", apiVersion)
        Object parsedResponse = MAPPER.readValue(response.body().string(), Object.class)

        then:
        response.successful == successfulResponse
        parsedResponse.errorCode == apiErrorCode

        where:
        testApiVersion | successfulResponse | apiErrorCode
        "FAKE"         | false              | "api.error.api-version.unsupported"
        "1040"         | false              | "api.error.api-version.unsupported"
        "0"            | false              | "api.error.api-version.unsupported"
        "0000001"      | false              | "api.error.api-version.unsupported"

    }

    def "test-resource"(){
        given:
        def client = getClient()
        def testResourceNode = "test-node"
        def projectName = "test-resource" // delete me
        def projectMapJson = [
                "name":projectName
        ]
        // Create test project with resources
        def createResponse = createSampleProject(projectMapJson)
        assert createResponse.successful
        setupProjectArchiveDirectoryResource(projectName, "/projects-import/resourcesTest")

        //Extract local nodename
        def systemInfoResponse = doGet("/system/info")
        SystemInfo systemInfo = MAPPER.readValue(systemInfoResponse.body().string(), SystemInfo.class)
        def localNode = systemInfo.system?.rundeck?.node

        when: "Obtain project's resources data in JSON"
        def localResourceNodeResponse = doGet("/project/$projectName/resource/$localNode")
        def localResourceString = localResourceNodeResponse.body().string()
        def parsedLocalNode = MAPPER.readValue(localResourceString, Map<String, Map>.class)
        def nodes = parsedLocalNode.collectEntries { nodeId, nodeData -> [nodeId, MAPPER.convertValue(nodeData, Node)] }
        def localNodeProps = nodes[localNode]

        then:
        localResourceNodeResponse.successful
        localNodeProps.nodename == localNode

        when: "Obtain project's resources data in YAML"
        def localResourceNodeResponseYaml = doGet("/project/$projectName/resource/$localNode?format=yaml")
        def localResourceYamlString = localResourceNodeResponseYaml.body().string()
        def newYaml = new Yaml().load(localResourceYamlString)

        then: "no errors and regex validation for YAML"
        noExceptionThrown()
        newYaml != null
        isYamlValid(localResourceYamlString)

        when: "If we attempt to use the response as JSON we'll get an exception"
        def localResourceNodeResponseYamlForJson = doGet("/project/$projectName/resource/$localNode?format=yaml")
        def localResourceYamlStringForJson = localResourceNodeResponseYamlForJson.body().string()
        MAPPER.readValue(localResourceYamlStringForJson, Map<String, Map>.class)

        then:
        thrown Exception

        when: "We try to get a specific node from project"
        def specificResourceResponse = doGet("/project/$projectName/resource/$testResourceNode")
        def specificResourceResponseString = specificResourceResponse.body().string()
        def parsedSpecificResourceResponseString = MAPPER.readValue(specificResourceResponseString, Map<String, Map>.class)
        def specificNodes = parsedSpecificResourceResponseString.collectEntries { nodeId, nodeData -> [nodeId, MAPPER.convertValue(nodeData, Node)] }
        def specificNode = specificNodes[testResourceNode]

        then:
        specificNode != null
        specificNode.nodename == testResourceNode

        cleanup:
        deleteProject(projectName)

    }

    def "test-resources"(){
        given:
        def client = getClient()
        def projectName = "test-resources" // delete me
        def projectMapJson = [
                "name":projectName
        ]
        // Create test project with resources
        def createResponse = createSampleProject(projectMapJson)
        assert createResponse.successful
        setupProjectArchiveDirectoryResource(projectName, "/projects-import/resourcesTest")

        //Extract local nodename
        def systemInfoResponse = doGet("/system/info")
        SystemInfo systemInfo = MAPPER.readValue(systemInfoResponse.body().string(), SystemInfo.class)
        def localNode = systemInfo.system?.rundeck?.node

        when: "Obtain all the nodes wait a bit"
        def allNodesResponse = client.doGetAcceptAll("/project/$projectName/resources")
        assert allNodesResponse.successful
        def allNodesResponseString = allNodesResponse.body().string()
        def parsedAllNodes = MAPPER.readValue(allNodesResponseString, Map<String, Map>.class)
        def allNodes = parsedAllNodes.collectEntries { nodeId, nodeData -> [nodeId, MAPPER.convertValue(nodeData, Node)] }

        then: "We check tha nodes qty and the format of the repsonse (must be json)"
        allNodes.size() > 1
        !isYamlValid(allNodesResponseString) // is not yaml
        parsedAllNodes != null // is json

        then: "Is we request the yaml format, the api delivers"
        def allNodesResponseYaml = client.doGetAcceptAll("/project/$projectName/resources?format=yaml")
        assert allNodesResponseYaml.successful
        def allNodesResponseYamlString = allNodesResponseYaml.body().string()
        def parsedAllNodesYaml = new Yaml().load(allNodesResponseString)

        then:
        isYamlValid(allNodesResponseYamlString)
        parsedAllNodesYaml != null

        when: "We try to parse yaml into JSON"
        MAPPER.readValue(allNodesResponseYamlString, Object.class)

        then: "Exeception thrown"
        thrown Exception

        when: "We request resources with unsupported format"
        def allNodesResponseInvalidApi = client.doGetAcceptAll("/project/$projectName/resources?format=sandwich")
        def parsedAllNodesResponseInvalidApi = MAPPER.readValue(allNodesResponseInvalidApi.body().string(), Object.class)

        then: "The api will return error code"
        parsedAllNodesResponseInvalidApi.errorCode == "api.error.resource.format.unsupported"
        parsedAllNodesResponseInvalidApi.error
        parsedAllNodesResponseInvalidApi.message?.contains("The format specified is unsupported: sandwich")

        when: "We test node filters to include a tag"
        def allNodesResponseFilteredByTag = client.doGetAcceptAll("/project/$projectName/resources?tags=testBoth")
        assert allNodesResponseFilteredByTag.successful
        def allNodesResponseFilteredByTagString = allNodesResponseFilteredByTag.body().string()
        def parsedNodesResponseFilteredByTagString = MAPPER.readValue(allNodesResponseFilteredByTagString, Map<String, Map>.class)
        def parsedNodesFilteredByTag = parsedNodesResponseFilteredByTagString.collectEntries { nodeId, nodeData -> [nodeId, MAPPER.convertValue(nodeData, Node)] }

        then: "Must be 2 of the 3 in total"
        parsedNodesFilteredByTag.size() == 2

        when: "We test node filters to include a tag"
        def allNodesResponseExcludedByTag = client.doGetAcceptAll("/project/$projectName/resources?exclude-tags=testBoth")
        assert allNodesResponseExcludedByTag.successful
        def allNodesResponseExcludedByTagString = allNodesResponseExcludedByTag.body().string()
        def parsedNodesResponseExcludedByTagString = MAPPER.readValue(allNodesResponseExcludedByTagString, Map<String, Map>.class)
        def parsedNodesExcludedByTag = parsedNodesResponseExcludedByTagString.collectEntries { nodeId, nodeData -> [nodeId, MAPPER.convertValue(nodeData, Node)] }

        then: "Must be 1 (localhost) of the 3 in total"
        parsedNodesExcludedByTag.size() == 1
        parsedNodesExcludedByTag[localNode].nodename == localNode

        when: "We test node filters to include a tag and exclude one node"
        def mixedFilterResponse = client.doGetAcceptAll("/project/$projectName/resources?tags=testBoth&exclude-name=test-node")
        assert mixedFilterResponse.successful
        def mixedFilterResponseString = mixedFilterResponse.body().string()
        def parsedMixedFilterResponseString = MAPPER.readValue(mixedFilterResponseString, Map<String, Map>.class)
        def parsedMixedFilter = parsedMixedFilterResponseString.collectEntries { nodeId, nodeData -> [nodeId, MAPPER.convertValue(nodeData, Node)] }

        then: "Must be 1 (test-node2) of the 3 in total"
        parsedMixedFilter.size() == 1
        parsedMixedFilter["test-node2"].nodename == "test-node2"

        when: "We test node filters to include a tag and exclude one node and exclude precedece to false"
        def mixedFilterWithPrecedenceResponse = client.doGetAcceptAll("/project/$projectName/resources?tags=test1&exclude-tags=testBoth&exclude-precedence=false")
        assert mixedFilterWithPrecedenceResponse.successful
        def mixedFilterWithPrecedenceResponseString = mixedFilterWithPrecedenceResponse.body().string()
        def parsedMixedFilterWithPrecedenceResponseString = MAPPER.readValue(mixedFilterWithPrecedenceResponseString, Map<String, Map>.class)
        def parsedMixedFilterWithPrecedence = parsedMixedFilterWithPrecedenceResponseString.collectEntries { nodeId, nodeData -> [nodeId, MAPPER.convertValue(nodeData, Node)] }

        then: "Must be 1 (test-node2) of the 3 in total"
        parsedMixedFilterWithPrecedence.size() == 1
        parsedMixedFilterWithPrecedence[localNode].nodename == localNode

        cleanup:
        deleteProject(projectName)

    }

    def "test-v23-project-source-resources"() {
        given:
        def client = getClient()
        def projectName = "test-project-resources" // delete me
        def resourceFile1 = "/home/rundeck/writable-resource-file.xml"
        def resourceFile2 = "/home/rundeck/test-resources.xml"
        Object projectJsonMap = [
            "name"  : projectName,
            "config": [
                "resources.source.1.config.file"                     : resourceFile1,
                "resources.source.1.config.format"                   : "resourcexml",
                "resources.source.1.config.generateFileAutomatically": "false",
                "resources.source.1.config.includeServerNode"        : "false",
                "resources.source.1.config.requireFileExists"        : "false",
                "resources.source.1.config.writeable"                : "true",
                "resources.source.1.type"                            : "file",
                "resources.source.2.config.file"                     : resourceFile2,
                "resources.source.2.config.format"                   : "resourcexml",
                "resources.source.2.config.generateFileAutomatically": "true",
                "resources.source.2.config.includeServerNode"        : "true",
                "resources.source.2.config.requireFileExists"        : "false",
                "resources.source.2.config.writeable"                : "false",
                "resources.source.2.type"                            : "file"
            ]
        ]

        def responseProject = createSampleProject(projectJsonMap)
        assert responseProject.successful

        //Extract local nodename
        def systemInfoResponse = doGet("/system/info")
        SystemInfo systemInfo = MAPPER.readValue(systemInfoResponse.body().string(), SystemInfo.class)
        def localNode = systemInfo.system?.rundeck?.node

        when: "we request all sources"
        def allSourcesResponse = client.doGetAcceptAll("/project/$projectName/sources")
        assert allSourcesResponse.successful
        List<ProjectSource> sources = MAPPER.readValue(allSourcesResponse.body().string(), ArrayList<ProjectSource>.class)

        then: "We can check them"
        sources.size() == 2
        sources[0].resources.writeable
        sources[0].resources.empty
        !sources[1].resources.writeable
        !sources[1].resources.empty


        when: "Request resources from the first source. Should be empty"
        def firstSourceResponse = client.doGetAcceptAll("/project/$projectName/source/1/resources")
        assert firstSourceResponse.successful
        def firstSourceResponseString = firstSourceResponse.body().string()
        Map<String, Node> source1Map = MAPPER.readValue(firstSourceResponseString, new TypeReference<Map<String, Node>>() {})
        def source1 = source1Map.mynode1


        then: "Check we get no resources"
        firstSourceResponseString == "{}"
        source1Map.isEmpty()
        source1 == null


        when: "Request resources from the second source and check contents"
        def secondSourceResponse = client.doGetAcceptAll("/project/$projectName/source/2/resources")
        assert secondSourceResponse.successful
        def secondSourceResponseString = secondSourceResponse.body().string()
        Map<String, Node> source2Map = MAPPER.readValue(secondSourceResponseString, new TypeReference<Map<String, Node>>() {})
        def localNodeProps = source2Map[localNode]
        def node1 = source2Map.node1

        then: "We can check the attributes"
        localNodeProps.nodename == localNode
        localNodeProps.hostname == localNode
        !localNodeProps.osFamily.isEmpty()
        !localNodeProps.osName.isEmpty()
        !localNodeProps.osArch.isEmpty()
        !localNodeProps.username.isEmpty()
        localNodeProps.description == "Rundeck server node"
        // check node1
        node1.nodename == "node1"
        node1.hostname == "hostname1"
        node1.osName == "osName1"
        node1.osFamily == "osFamily1"
        node1.osArch == "osArch1"
        node1.osVersion == "osVersion1"
        node1.username == "username1"
        node1.tags == "tag1, tag2"
        node1.attributes.size() >= 5
        node1.attributes.testattribute == "testvalue"
        node1.attributes.testattribute2  == "test value2"
        node1.attributes.testattribute3 == "test value3"
        node1.attributes.editUrl == "EditURL"
        node1.attributes.remoteUrl == "RemoteURL"


        when: "Update the first source with adding a new node"
        def nodeUpdateResponse = client.doPost("/project/$projectName/source/1/resources", [
            "mynode1": [
                "nodename"   : "mynode1",
                "description": "Updated node",
                "tags"       : "api, test",
                "hostname"   : "mynode1",
                "osArch"     : "arm64",
                "osFamily"   : "linux",
                "osName"     : "Debian",
                "osVersion"  : "1.0.0",
                "username"   : "myusername",
                "attr1"      : "testvalue",
                "attr2"      : "testvalue2"
            ]
        ])

        then:
        assert nodeUpdateResponse.successful

        when: "We request the updated node"
        def updatedNodeResponse = client.doGetAcceptAll("/project/$projectName/source/1/resources")
        def updatedNodeResponseString = updatedNodeResponse.body().string()
        Map<String, Node> updatedNodeResponseMap = MAPPER.readValue(updatedNodeResponseString, new TypeReference<Map<String, Node>>() {})

        then:
        !updatedNodeResponseMap.isEmpty()
        updatedNodeResponseMap.mynode1 != null
        updatedNodeResponseMap.mynode1.nodename == "mynode1"
        updatedNodeResponseMap.mynode1.description == "Updated node"
        updatedNodeResponseMap.mynode1.tags == "api, test"
        updatedNodeResponseMap.mynode1.hostname == "mynode1"
        updatedNodeResponseMap.mynode1.osArch == "arm64"
        updatedNodeResponseMap.mynode1.osFamily == "linux"
        updatedNodeResponseMap.mynode1.osName == "Debian"
        updatedNodeResponseMap.mynode1.osVersion == "1.0.0"
        updatedNodeResponseMap.mynode1.username == "myusername"
        updatedNodeResponseMap.mynode1.attributes.size() == 2
        updatedNodeResponseMap.mynode1.attributes.attr1 == "testvalue"
        updatedNodeResponseMap.mynode1.attributes.attr2 == "testvalue2"

        cleanup:
        deleteProject(projectName)

    }

    def "test-v23-project-sources-json"(){
        given:
        def projectName = "test-project-sources-json"
        def client = getClient()
        Object projectJsonMap = [
                "name": projectName
        ]

        def responseProject = createSampleProject(projectJsonMap)
        assert responseProject.successful

        when: "We request source 1"
        def allSourcesResponse = client.doGetAcceptAll("/project/$projectName/sources")
        assert allSourcesResponse.successful
        def allSourcesResponseString = allSourcesResponse.body().string()
        List<ProjectSource> sources = MAPPER.readValue(allSourcesResponseString, ArrayList<ProjectSource>.class)

        then: "The source will be in json format and will have properties"
        !isYamlValid(allSourcesResponseString)
        sources.size() == 1
        sources != null
        sources[0].index > 0
        sources[0].resources != null
        !sources[0].resources.writable
        sources[0].resources.href.contains("/source/1/resources")

        cleanup:
        deleteProject(projectName)
    }

    def createSampleProject = (Object projectJsonMap) -> {
        return client.doPost("/projects", projectJsonMap)
    }

    boolean isYamlValid(String yamlString) {
        def yamlRegex = /^(?:\s*[\w-]+(\s*:\s*(?:(?:\s*".*?"\s*)|(?:\s*'.*?'\s*)|(?:.*?))\s*)?\s*)+$/
        yamlString =~ yamlRegex
    }

}
