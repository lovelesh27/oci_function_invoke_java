import oci
import logging
from fdk import response

def handler(ctx, data: io.BytesIO = None):
    try:
        logging.getLogger().info("Function handler triggered, try.....")
    except (Exception, ValueError) as ex:
        logging.getLogger().info('error parsing json payload: ' + str(ex))

    logging.getLogger().info("Function func2 handler triggered.....")
    trigger_function()
    return response.Response(
        ctx, response_data=json.dumps(
            {"Result": "Function executed successfully."}),
        headers={"Content-Type": "application/json"}
    )

def trigger_function():
    logging.getLogger().info("Reached python function func2...")
