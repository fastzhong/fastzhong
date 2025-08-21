# Architecture Overview
🔹 Two Data Centers
Production (ADC) and Disaster Recovery (TDC) are set up in an active-active configuration.
🔹 Core Components
- Global Load Balancer
- Local Load Balancer

Entry point for external file transfers.
RFTS Edge Servers

Located in the DMZ.
Handle external connections securely.
Use PostgreSQL for local data.
RFTS App Clusters

SFTP App Cluster.
Handle internal file processing and protocol-specific tasks.
Use Oracle DB for configuration and transaction data.
Oracle DB

Centralized database for App Servers.
Replicated between ADC and TDC using Oracle Data Guard.
Data File Store (NAS)

Shared storage for actual files being transferred.
Accessible by App Servers in both data centers.
App DP Server

Likely handles data processing or orchestration tasks.
🔹 Connectivity
Load Balancer connects external users to the system.
Solid lines indicate data flow.
Dashed lines show replication paths (e.g., Oracle Data Guard, NAS sync).
Green components are new; gray components are existing.
