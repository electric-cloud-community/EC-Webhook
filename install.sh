pdk generate plugin
pdk build
ectool deletePlugin EC-Webhook-1.0.1.0
ectool deleteProject EC-Webhook-1.0.1.0
ectool installPlugin build/EC-Webhook.zip
ectool promotePlugin EC-Webhook-1.0.1.0
