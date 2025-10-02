# FEK Stack Centralized Logging

This document describes the FEK (Fluent-bit, Elasticsearch, Kibana) stack implementation for the Face Microservice project.

## Stack Components

### 🔍 Elasticsearch 8.11.0
- **Purpose**: Log storage and search engine
- **Port**: 9200 (HTTP API), 9300 (Transport)
- **Data**: Stored in `elasticsearch-data` volume
- **Memory**: 1-2GB allocated

### 📊 Kibana 8.11.0
- **Purpose**: Log visualization and analysis interface
- **Port**: 5601
- **URL**: http://localhost:5601

### 📋 Fluent-bit 3.1.9
- **Purpose**: Lightweight log processor and forwarder
- **Data Source**: Docker container logs via fluentd logging driver
- **Output**: Elasticsearch

## Quick Start

### 1. Setup FEK Stack
```bash
# Run the setup script
./setup-fek.sh

# Or manually start services
docker compose up -d elasticsearch kibana fluent-bit
```

### 2. Access Kibana
1. Open http://localhost:5601 in your browser
2. Go to **Stack Management** → **Index Patterns**
3. Create index pattern: `face-microservice-logs-*`
4. Select `@timestamp` as time field
5. Go to **Discover** to view logs

### 3. Start Application Services
All application services are already configured with fluentd logging driver:
```bash
# Start all application services
docker compose up -d ai-backend-service auth-service user-service face-reg-engine face-regconition-service app-fe
```

## Log Analysis

### Kibana Discover Queries
```
# Filter by service
container.name: "/face-microservice-ai-backend-service-1"

# Filter by log level
level: "ERROR" OR level: "error"

# Filter by time range and service
@timestamp:[now-1h TO now] AND container.name: "*ai-backend*"

# Search in message content
message: "database" OR message: "connection"

# Combine filters
level: "ERROR" AND container.name: "*ai-backend*" AND @timestamp:[now-24h TO now]
```

### Elasticsearch Direct Queries
```bash
# Cluster health
curl http://localhost:9200/_cluster/health?pretty

# List all indices
curl http://localhost:9200/_cat/indices?v

# Search for errors
curl -X GET "localhost:9200/face-microservice-logs-*/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {
    "bool": {
      "must": [
        {"match": {"level": "ERROR"}},
        {"range": {"@timestamp": {"gte": "now-1h"}}}
      ]
    }
  },
  "sort": [{"@timestamp": {"order": "desc"}}],
  "size": 10
}'

# Get logs from specific service
curl -X GET "localhost:9200/face-microservice-logs-*/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {
    "wildcard": {
      "container.name": "*ai-backend*"
    }
  },
  "sort": [{"@timestamp": {"order": "desc"}}],
  "size": 5
}'
```

## Index Management

### Index Template
The setup creates an index template `face-microservice-logs` that:
- Matches pattern: `face-microservice-logs-*`
- Daily indices: `face-microservice-logs-YYYY.MM.DD`
- Single shard, no replicas (for development)
- Proper field mappings for timestamps, levels, containers

### Index Lifecycle Management (ILM)
```bash
# Check ILM policy
curl http://localhost:9200/_ilm/policy/face-microservice-policy?pretty

# Manual index rollover
curl -X POST "localhost:9200/face-microservice-logs-alias/_rollover?pretty"
```

## Troubleshooting

### Common Issues

1. **Elasticsearch won't start**
   ```bash
   # Check vm.max_map_count
   sudo sysctl vm.max_map_count
   
   # Set if needed
   sudo sysctl -w vm.max_map_count=262144
   ```

2. **No logs appearing**
   ```bash
   # Check Filebeat status
   docker compose logs filebeat
   
   # Check Elasticsearch indices
   curl http://localhost:9200/_cat/indices?v
   
   # Verify Docker socket permissions
   ls -la /var/run/docker.sock
   ```

3. **High memory usage**
   ```bash
   # Reduce Elasticsearch heap size in docker-compose.yml
   ES_JAVA_OPTS: "-Xms512m -Xmx512m"
   ```

### Log Validation
```bash
# Check if logs are flowing
curl -s "http://localhost:9200/face-microservice-logs-*/_count" | jq '.'

# Get latest logs
curl -X GET "localhost:9200/face-microservice-logs-*/_search?pretty&size=1&sort=@timestamp:desc"
```

### Performance Monitoring
```bash
# Cluster stats
curl http://localhost:9200/_cluster/stats?pretty

# Node stats
curl http://localhost:9200/_nodes/stats?pretty

# Index stats
curl http://localhost:9200/face-microservice-logs-*/_stats?pretty
```

## Configuration Files

- `docker-compose.yml`: Service definitions
- `logging/fluent-bit-simple.conf`: Fluent-bit configuration
- `setup-fek.sh`: Automated setup script

## Benefits of FEK Stack

1. **Rich Query Language**: Elasticsearch Query DSL for powerful searches
2. **Advanced Analytics**: Kibana visualizations and dashboards
3. **Industry Standard**: Wide adoption and community support
4. **Scalability**: Better horizontal scaling capabilities
5. **Machine Learning**: Built-in anomaly detection (X-Pack)
6. **APM Integration**: Application Performance Monitoring

## Resource Usage

- **Elasticsearch**: ~1-2GB RAM (adjustable)
- **Kibana**: ~512MB-1GB RAM  
- **Fluent-bit**: ~50-128MB RAM (lightweight)
- **Total**: ~2-3GB RAM

Note: FEK stack provides comprehensive logging capabilities with efficient resource usage.