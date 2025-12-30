# Host remote MCP servers built with official MCP SDKs on Azure Functions (public preview)

The repo contains instructions and sample for running a Model Context Protocol (MCP) server built with Java/Quarkus on Azure Functions. The sample is a simple weather server that you can clone to run locally and easily deploy to the cloud with the Azure Developer CLI. 

## Running MCP server as custom handler on Azure Functions

Recently Azure Functions released the [Functions MCP extension](https://techcommunity.microsoft.com/blog/appsonazureblog/build-ai-agent-tools-using-remote-mcp-with-azure-functions/4401059), allowing developers to build MCP servers using Functions programming model, which is essentially Function's event-driven framework, and host them remotely on the serverless platform.

For those who have already built servers with [Anthropic's MCP SDKs](https://github.com/modelcontextprotocol/servers?tab=readme-ov-file#model-context-protocol-servers), it's also possible to host the servers on Azure Functions by running them as _custom handlers_, which are lightweight web servers that receive events from the Functions host. They allow you to host your already-built MCP servers with no code changes and benefit from Function's bursty scale, serverless pricing model, and security features.

This repo focuses on the second hosting scenario:  

<div align="center">
  <img src="./media/function_hosting.png" alt="Diagram showing hosting of Function app and custom handler apps." width="500">
</div

## What’s here

- `quarkus-mcp-server/`: Quarkus MCP server packaged as an uber‑jar
- `e2e/`: Minimal Node-based end‑to‑end test runner

## Prerequisites 

* Java 17, Maven 3.8 or above
* Node.js 18 or above (for E2E test)
* [Azure subscription](https://azure.microsoft.com/free/dotnet/) (you can create one for free)
* [Azure Developer CLI (azd)](https://learn.microsoft.com/azure/developer/azure-developer-cli/install-azd) v1.17.2 or above
* [Azure Functions Core Tools](https://learn.microsoft.com/azure/azure-functions/functions-run-local?tabs=windows%2Cisolated-process%2Cnode-v4%2Cpython-v2%2Chttp-trigger%2Ccontainer-apps&pivots=programming-language-typescript) v4.5.0 or above
* [Visual Studio Code](https://code.visualstudio.com/)
* [Azure Functions extension on Visual Studio Code](https://marketplace.visualstudio.com/items?itemName=ms-azuretools.vscode-azurefunctions)

## Test the server locally

1. In the `quarkus-mcp-server` directory, build the server with debug profile: 
  ```shell
  mvn clean package -P debug
  ```
1. Navigate to the `target/azure-functions/quarkus-mcp` directory then start the server:
  ```shell
  func start
  ```
1. Open _.vscode/mcp.json_ and start the server by clicking the _Start_ button above the **local-mcp-server**
1. Click on the Copilot icon at the top to open chat (or `Ctrl+Command+I / Ctrl+Alt+I`), and then change to _Agent_ mode in the question window.
1. Click the tools icon and make sure **local-mcp-server** is checked for Copilot to use in the chat:
   
    <img src="./media/mcp-tools.png" width="200" alt="MCP tools list screenshot">
1. Once the server displays the number of tools available, ask "Return the weather in NYC using #local-mcp-server." Copilot should call one of the weather tools to help answer this question.

## Deployment 

1. In the `quarkus-mcp-server` directory, build the server for deployment: 
    ```shell
    mvn clean package
    ```
1. Create a new azd project environment. The name of the environment becomes resource group name where Azure resources are deployed to: 
    ```shell
    azd env new <environment-name>
    ```
1. This sample uses Visual Studio Code as the main client. Configure it as an allowed client application:
    ```shell
    azd env set PRE_AUTHORIZED_CLIENT_IDS aebc6443-996d-45c2-90f0-388ff96faa56
    ```
    >[!NOTE]
    >Microsoft employees using a Microsoft tenant must provide a service management reference (your Service Tree ID). Without it you won't be able to create the Entra app registration, and provisioning will fail. 
    > 
    > Set the SERVICE_MANAGEMENT_REFERENCE just like above. 

1. Run `azd up` in the root directory. Then pick an Azure subcription to deploy resources to and select from the available regions (e.g. East US). Enable vnet if preferred. 

    When the deployment finishes, your terminal will display output similar to the following:

    ```shell
      (✓) Done: Resource group: rg-resource-group-name (12.061s)
      (✓) Done: App Service plan: plan-random-guid (6.748s)
      (✓) Done: Virtual Network: vnet-random-guid (8.566s)
      (✓) Done: Log Analytics workspace: log-random-guid (29.422s)
      (✓) Done: Storage account: strandomguid (34.527s)
      (✓) Done: Application Insights: appi-random-guid (8.625s)
      (✓) Done: Function App: func-mcp-random-guid (36.096s)
      (✓) Done: Private Endpoint: blob-private-endpoint (30.67s)

      Deploying services (azd deploy)
      (✓) Done: Deploying service mcp
      - Endpoint: https://functionapp-name.azurewebsites.net/
    ```
### Connect to server on Visual Studio Code

1. Open _.vscode/mcp.json_ in the editor.
1. Stop the local server by clicking the _Stop_ button above the **local-mcp-server**.
1. Start the remote server by clicking the _Start_ button above the **remote-mcp-server**.
1. Visual Studio Code will prompt you for the Function App domain. Copy it from either the terminal output or the Portal.
1. Open Copilot in Agent mode and make sure **remote-mcp-server** is checked in the tool's list.
1. VS Code should prompt you to authenticate to Microsoft. Click _Allow_, and then login into your Microsoft account (the one used to access Azure Portal).
1. Ask Copilot "Return the weather in Seattle using #remote-mcp-server". It should call one of the weather tools to help answer.

>[!TIP]
>In addition to starting an MCP server in _mcp.json_, you can see output of a server by clicking _More..._ -> _Show Output_. The output provides useful information like why a connection might've failed.
>
>You can also click the gear icon to change log levels to "Traces" to get even more details on the interactions between the client (Visual Studio Code) and the server.
>
><img src="./media/log-level.png" width="200" alt="Log level screenshot">

### Built-in server authentication and authorization 

The server app is configured with the built-in server authentication and authorization feature, which implements the requirements of the [MCP authorization specification](https://modelcontextprotocol.io/specification/2025-06-18/basic/authorization#authorization-server-discovery), such as issuing 401 challenge and exposing a Protected Resource Metadata (PRM). 

In the debug output from Visual Studio Code, you see a series of requests and responses as the MCP client and server interact. When built-in MCP server authorization is used, you should see the following sequence of events:

1. The editor sends an initialization request to the MCP server.
1. The MCP server responds with an error indicating that authorization is required. The response includes a pointer to the protected resource metadata (PRM) for the application. The built-in authorization feature generates the PRM for the server app.
1. The editor fetches the PRM and uses it to identify the authorization server.
1. The editor attempts to obtain authorization server metadata (ASM) from a well-known endpoint on the authorization server.
1. Microsoft Entra ID doesn't support ASM on the well-known endpoint, so the editor falls back to using the OpenID Connect metadata endpoint to obtain the ASM. It tries to discover this using by inserting the well-known endpoint before any other path information.
1. The OpenID Connect specifications actually defined the well-known endpoint as being after path information, and that is where Microsoft Entra ID hosts it. So the editor tries again with that format.
1. The editor successfully retrieves the ASM. It then uses this information in conjunction with its own client ID to perform a login. At this point, the editor prompts you to sign in and consent to the application.
1. Assuming you successfully sign in and consent, the editor completes the login. It repeats the intialization request to the MCP server, this time including an authorization token in the request. This re-attempt isn't visible at the Debug output level, but you can see it in the Trace output level.
1. The MCP server validates the token and responds with a successful response to the initialization request. The standard MCP flow continues from this point, ultimately resulting in discovery of the MCP tool defined in this sample.

### Redeployment

If you want to redeploy the server after making changes, run `azd deploy`. (See azd command [reference](https://learn.microsoft.com/azure/developer/azure-developer-cli/reference).)

## E2E test

```powershell
cd e2e
npm install
npm test
```

## Notes

- HTTP binds to 0.0.0.0 and uses FUNCTIONS_CUSTOMHANDLER_PORT
- `host*.json` templates resolve the runner JAR name at package time
