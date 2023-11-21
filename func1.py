import oci
import logging
from fdk import response

def handler(ctx, data: io.BytesIO = None):
    try:
        logging.getLogger().info("Function handler triggered, try.....")
    except (Exception, ValueError) as ex:
        logging.getLogger().info('error parsing json payload: ' + str(ex))

    logging.getLogger().info("Function func1 handler triggered.....")
    trigger_function()
    return response.Response(
        ctx, response_data=json.dumps(
            {"Result": "Function executed successfully."}),
        headers={"Content-Type": "application/json"}
    )

def trigger_function():
    logging.getLogger().info("Reached python function func1...")
    function_endpoint = "https://932jksmn9.us-ashburn-1.functions.oci.oraclecloud.com"
    function_ocid = "ocid1.fnfunc.oc1.iad.aaaaaaaad2ugdks"
    function_body = "Hii"
    signer = oci.auth.signers.get_resource_principals_signer()
    client = oci.functions.FunctionsInvokeClient(config={}, signer=signer, service_endpoint=function_endpoint)
    resp = client.invoke_function(function_id=function_ocid, invoke_function_body=function_body)
    logging.getLogger().info("Completed calling func2")
