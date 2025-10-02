#!/bin/bash

# FEK Stack Setup Script for Face Microservice
# This script sets up Fluent-bit, Elasticsearch, and Kibana for centralized logging

set -e

echo "🔧 Setting up FEK Stack for Face Microservice..."

# Create logging directory if it doesn't exist
mkdir -p ./logging

# Set proper permissions for Elasticsearch data directory
echo "📁 Setting up Elasticsearch data permissions..."
sudo sysctl -w vm.max_map_count=262144
echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf

# Start Elasticsearch first
echo "🟢 Starting Elasticsearch..."
docker compose up -d elasticsearch

# Wait for Elasticsearch to be ready
echo "⏳ Waiting for Elasticsearch to be ready..."
timeout=120
counter=0
while ! curl -s http://localhost:9200/_health >/dev/null 2>&1; do
    if [ $counter -ge $timeout ]; then
        echo "❌ Elasticsearch failed to start within $timeout seconds"
        exit 1
    fi
    echo "Waiting for Elasticsearch... ($counter/$timeout)"
    sleep 5
    counter=$((counter + 5))
done

echo "✅ Elasticsearch is ready!"

# Start Kibana
echo "🟢 Starting Kibana..."
docker compose up -d kibana

# Wait for Kibana to be ready
echo "⏳ Waiting for Kibana to be ready..."
timeout=120
counter=0
while ! curl -s http://localhost:5601/api/status >/dev/null 2>&1; do
    if [ $counter -ge $timeout ]; then
        echo "❌ Kibana failed to start within $timeout seconds"
        exit 1
    fi
    echo "Waiting for Kibana... ($counter/$timeout)"
    sleep 5
    counter=$((counter + 5))
done

echo "✅ Kibana is ready!"

# Start Fluent-bit
echo "🟢 Starting Fluent-bit..."
docker compose up -d fluent-bit

# Wait for Fluent-bit to be ready
echo "⏳ Waiting for Fluent-bit to start..."
sleep 10

# Create index template and initial index
echo "📋 Setting up Elasticsearch index template..."
curl -X PUT "localhost:9200/_index_template/face-microservice-logs" \
  -H "Content-Type: application/json" \
  -d '{
    "index_patterns": ["face-microservice-logs-*"],
    "template": {
      "settings": {
        "number_of_shards": 1,
        "number_of_replicas": 0
      },
      "mappings": {
        "properties": {
          "@timestamp": {
            "type": "date"
          },
          "message": {
            "type": "text",
            "fields": {
              "keyword": {
                "type": "keyword",
                "ignore_above": 256
              }
            }
          },
          "level": {
            "type": "keyword"
          },
          "container": {
            "properties": {
              "name": {
                "type": "keyword"
              },
              "id": {
                "type": "keyword"
              }
            }
          },
          "host": {
            "properties": {
              "name": {
                "type": "keyword"
              }
            }
          },
          "cluster": {
            "type": "keyword"
          },
          "environment": {
            "type": "keyword"
          }
        }
      }
    }
  }'

echo ""
echo "🎉 FEK Stack setup completed successfully!"
echo ""
echo "📊 Access URLs:"
echo "   - Elasticsearch: http://localhost:9200"
echo "   - Kibana:        http://localhost:5601"
echo ""
echo "📖 Next steps:"
echo "   1. Access Kibana at http://localhost:5601"
echo "   2. Go to Stack Management > Index Patterns"
echo "   3. Create index pattern: face-microservice-logs-*"
echo "   4. Select @timestamp as time field"
echo "   5. Go to Discover to view logs"
echo ""
echo "🔍 Useful Elasticsearch queries:"
echo "   # Check cluster health"
echo "   curl http://localhost:9200/_cluster/health"
echo ""
echo "   # List indices"
echo "   curl http://localhost:9200/_cat/indices?v"
echo ""
echo "   # Search logs"
echo "   curl \"http://localhost:9200/face-microservice-logs-*/_search?pretty&q=level:ERROR\""
echo ""