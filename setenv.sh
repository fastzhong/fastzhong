#! /bin/bash
java_bin=/app/java/jdk-17.0.9/bin
base_dir=/prodlib/CEWMSTH
services_dir=$base_dir/services
log_dir=/prodlib/CEWMSTH/logs
conf_dir=$base_dir/conf
scripts_dir=$base_dir/scripts
pid_dir=/prodlib/CEWMSTH/pid
ssl_dir=/prodlib/CEWMSTH/conf/ssl_cert
common_dir=/prodlib/CEWMSTH/conf/common

source $( cd -P -- "$(dirname -- "$(command -v -- "$0")")" && pwd -P )/setmq.sh

# port - required for TH
ueqs_port=6685
ueqs_routes_1=6687
ueqs_routes_2=6685
ueqs_routes_3=6685
rds_port=6682
bms_port=6683
twe_port=6620
tws_port=6670
ans_port=6694
fus_port=6674
pis_ccti_port=6690
cnacomp_port=6630
cnaauth_port=6632
mcrs_port=6635
uecs_port=6671
domestic_port=6633
domestic_payroll_port=6634
managebatch_port=6673
bulk_process_port=6615
cna_bank_port=8031
aqs_port=6644

# port - Not required for TH
fx_port=6615
pmts_port=6691
payroll_port=6695
cbp_port=6610
pis_port=6690

# port  -others
idb_port=8607
sms_port=12104
email_port=12104
notification_port=12102

country=th
country_upper=$(echo $country | tr 'a-z' 'A-Z')

cna_services_dir=$services_dir/ca_company
bms_conf_dir=$base_dir/conf
bmsMockFilePath=$conf_dir/mock/

DL_PROP_PATH=$conf_dir/transactionworkflowservices/dailylimit.properties
file_upload_nfs_directory=/CEWTH/BULK/Outgoing/attachment/
export file_upload_lc=false
cbp_rulebookVersion="{#:2023 Edition 1}"
paymentrail_rulebookVersion="{#:2023 Edition 2}"
export cbp_rulebookVersion paymentrail_rulebookVersion

camelSSLContext=TLSv1.2

#JVM
Xms_value=512M
Xmx_value=512M
timezone=Asia/Bangkok
retentionPeriod=365

ufw_app_debug_response_enabled=true
ufw_app_debug_stack_trace_enabled=true

# hostname - self
cna_bank_host=lxcewpsgv001
cna_bank_host2=lxcewpsgv002
hostname=`hostname`

# hostname - others
sms_host=idbth-ms.sg.uobnet.com
email_host=idbth-ms.sg.uobnet.com
notification_host=idbth-ms.sg.uobnet.com

host1=LXCEWPSGV022
host2=LXCEWPSGV023

#Load balancer
lbMaximumFailoverAttempts=2
lbRoutesUeqs=https://$host1:$ueqs_routes_1,https://$host2:$ueqs_routes_1
lbRoutesPis=https://$host1:$pis_ccti_port,https://$host2:$pis_ccti_port
lbRoutesRds=https://$host1:$rds_port,https://$host2:$rds_port
lbRoutesBms=https://$host1:$bms_port,https://$host2:$bms_port
lbRoutesFx=https://$host1:$fx_port,https://$host2:$fx_port

export tweRouteServiceUrl=https://$hostname:$twe_port
export ueqsRouteServiceUrl=https://$hostname:$ueqs_port
export dwpfRouteServiceUrl=https://$hostname:$domestic_port
export dprwfRouteServiceUrl=https://$hostname:$domestic_payroll_port
export twfRouteServiceUrl=https://$hostname:$tws_port
export bmsRouteServiceUrl=https://$hostname:$bms_port
export mbsRouteServiceUrl=https://$hostname:$managebatch_port
export uecsRouteServiceUrl=https://$hostname:$uecs_port

## TODO
#load balancer, mahendra we need load balancer
#/etc/hosts
#cewth-sso1.sg.uobnet.com 1.1.1.1
#cewth-sso2.sg.uobnet.com 2.2.2.2

# Downstream Dependencies

export idbCallBackUrlBasePath=https://172.28.201.196:12106

user_bank_role_uri=/entitlementsapi/api/entitlements/v1/bank/user/userBanksAndRoles

#NPP
# Required the following  NPP endpoint for TH
npp_url_basepath=https://pomsctsvc.thnpp.sg.uobnet.com/pom-sct/v1
npp_url_blk_basepath=https://pomblksvc.thnpp.sg.uobnet.com/pomblk/v1
npp_utility_basepath=https://nppsg-converter-service.nppsg-sitqr.apps.ocpt001.sg.uobnet.com/pom-sct/v1
npp_earmarking_basepath=https://cbis-service.nppsg-sitqr.apps.ocpt001.sg.uobnet.com/cbis
npp_confirmquote_basepath=http://nttcatsgv93:60820
npp_unearmarking_basepath=https://cbis-service.nppsg-sitqr.apps.ocpt001.sg.uobnet.com
npp_blk_cnh_basepath=https://nppsg-converter-service.nppsg-sitqr.apps.ocpt001.sg.uobnet.com/pomblk/v1
npp_api_basepath=https://bvaadmin-qr.t-npp.sg.uobnet.com

#PDR
pdr_base_port=26621
pdr_basepath=https://pdrth-api.sg.uobnet.com:26621/pdradvice
pdr_accountenquiry_basepath=https://pdrpar-api.sg.uobnet.com:26521/accountlookupapi
pdr_customerreport_basepath=https://pdrth-api.sg.uobnet.com:26621
pdr_advicebasepath=
pdr_cert_alias=uobsend_sit-pdr

#EDT
edt_basepath=https://lxedttsgv98:9443
edt_host=10.85.100.40

# TTR -not applicable
# ttr_basepath=http://lxttrtsgv098:8080

#SSO
sso_hostpath=https://cewth-sso1.sg.uobnet.com:7284/am5/xmlrpc,https://cewth-sso2.sg.uobnet.com:7284/am5/xmlrpc
sso_attribute_val=jks
sso_mock_flag=false

# Vault
vault_keystore_salt=bs97kr35
vault_keystore_password=MASK-3/dtUlE1OTZxBxjGpxlMKH
vault_iteration_count=1888

keystore_alias_env=vault

#DB Wallet
aes_wallet_location=/prodlib/SCM/wallets/owncewth/cewpeth
bms_wallet_location=/prodlib/SCM/wallets/owncewth/cewpmth
ans_wallet_location=/prodlib/SCM/wallets/owncewth/cewpnth
rds_wallet_location=/prodlib/SCM/wallets/owncewth/cewpmth
pws_wallet_location=/prodlib/SCM/wallets/owncewth/cewppth
cas_wallet_location=/prodlib/SCM/wallets/owncewth/cewpbmy
ced_wallet_location=/prodlib/SCM/wallets/owncewth/cewpdth
aqs_wallet_location=/prodlib/SCM/wallets/owncewth/cewpmth
rds_db_alias=CEWPMTH
aes_db_alias=CEWPETH
bms_db_alias=CEWPMTH
ans_db_alias=CEWPNTH
pws_db_alias=CEWPPTH
cas_db_alias=CEWPBMY
ced_db_alias=CEWPDTH
aqs_db_alias=CEWPMTH
secondary_db_wallet_location=/prodlib/SCM/wallets/owncewth/cewpeth
secondary_db_alias=CEWPETH
tns_admin=/app/oraclient/19.3.0.0/client_1/network/admin


#Axway
axway_basepath=https://caa-oauth-th.sg.uobnet.com:7089/api/oauth/token
axway_client_id=CEW-TH

# ADFS
adfs_public_key_file=adfs-public-key-only.pub

# NFS
nfs_attachment_directory=/CEWTH/BULK/Outgoing/attachment/
done_nfs_directory=/CEWTH/BULK/Outgoing/processing/
ccti_nfs_attachment_directory=/CEWTH/BULK/Outgoing/attachment/
ccti_done_nfs_directory=/CEWTH/BULK/Outgoing/processing/
dmpIncomingProcessingFilePath=/CEWTH/BULK/Incoming/processing/
dmpIncomingProcessingFATEFilePath=/CEWTH/BULK/Incoming/FATE/
dmpIncomingBackupFilePath=/CEWTH/BULK/Incoming/backup/
dmpIncomingErrorFilePath=/CEWTH/BULK/Incoming/error/
bulkprocess_dmpIncomingProcessingFilePath=/CEWTH/BULK/Incoming/processing/
bulkprocess_dmpIncomingBackupFilePath=/CEWTH/backup/
bulkprocess_dmpIncomingErrorFilePath=/CEWTH/BULK/Incoming/error/

ccti_dmpIncomingProcessingFilePath=/CEWTH/BULK/Outgoing/processing1/
ccti_dmpIncomingProcessingFATEFilePath=/CEWTH/BULK/Incoming/FATE/
ccti_dmpIncomingBackupFilePath=/CEWTH/BULK/Incoming/backup1/
ccti_dmpIncomingErrorFilePath=/CEWTH/BULK/Incoming/error1/

report_upload_nfs_directory=/CEWTH/backup/

# ros
rosBulkOutgoingProcessingFilePath=/CEWTH/BULK/Incoming/error1/
rosBulkIncomingFATEFilePath=/CEWTH/BULK/Incoming/FATE/
rosBulkIncomingACKFilePath=/CEWTH/BULK/Incoming/FATE/
rosBulkIncomingErrorFilePath=/CEWTH/BULK/Incoming/error1/
rosBulkIncomingBackupFilePath=/CEWTH/backup1/
rosBulkIncomingACCFilePath=/CEWTH/backup1/
rosBulkIncomingREJFilePath=/CEWTH/backup1/

export nfs_attachment_directory done_nfs_directory dmpIncomingProcessingFilePath dmpIncomingProcessingFATEFilePath dmpIncomingBackupFilePath dmpIncomingErrorFilePath bulkprocess_dmpIncomingProcessingFilePath bulkprocess_dmpIncomingBackupFilePath bulkprocess_dmpIncomingErrorFilePath report_upload_nfs_directory ccti_nfs_attachment_directory ccti_done_nfs_directory ccti_dmpIncomingProcessingFilePath ccti_dmpIncomingProcessingFATEFilePath ccti_dmpIncomingBackupFilePath ccti_dmpIncomingErrorFilePath rosBulkOutgoingProcessingFilePath rosBulkIncomingFATEFilePath rosBulkIncomingACKFilePath rosBulkIncomingErrorFilePath rosBulkIncomingBackupFilePath rosBulkIncomingACCFilePath rosBulkIncomingREJFilePath

export sso_mock_flag file_upload_nfs_directory adfs_public_key_file pdr_cert_alias timezone

export keystore_alias_env wallet_location db_alias sso_attribute_val Xms_value Xmx_value axway_client_id secondary_db_wallet_location secondary_db_alias
export vault_iteration_count vault_keystore_password vault_keystore_salt camelSSLContext pdr_base_port
export ttr_basepath ueqs_port fx_port rds_port twe_port pis_port pmts_port country country_upper hostname bms_port tws_port ans_port mcrs_port ueqs_routes_1 ueqs_routes_2 ueqs_routes_3 payroll_port cbp_port idb_port sms_port notification_port fus_port pis_ccti_port sms_host notification_host tns_admin email_port email_host cnacomp_port cnaauth_port uecs_port domestic_port domestic_payroll_port managebatch_port cna_bank_port cna_bank_host cna_bank_host2 user_bank_role_uri retentionPeriod bulk_process_port aqs_port
export npp_api_basepath npp_url_basepath npp_url_blk_basepath npp_utility_basepath npp_earmarking_basepath npp_confirmquote_basepath npp_unearmarking_basepath npp_blk_cnh_basepath
export pdr_basepath pdr_customerreport_basepath edt_basepath edt_host axway_basepath sso_hostpath pdr_advicebasepath pdr_accountenquiry_basepath

echo "BaseDir: $base_dir ServicesDir: $services_dir LogDir: $log_dir ConfDir: $conf_dir ScriptsDir: $scripts_dir pidDir: $pid_dir  ssl_dir:$ssl_dir common_dir:$common_dir run_env:$run_env DL_PROP_PATH: $DL_PROP_PATH"
export base_dir services_dir log_dir conf_dir scripts_dir pid_dir ssl_dir common_dir run_env DL_PROP_PATH cna_services_dir

export aes_wallet_location bms_wallet_location ans_wallet_location rds_wallet_location pws_wallet_location cas_wallet_location ced_wallet_location aqs_wallet_location
export rds_db_alias aes_db_alias ans_db_alias bms_db_alias pws_db_alias cas_db_alias ced_db_alias aqs_db_alias

echo "lbMaximumFailoverAttempts:$lbMaximumFailoverAttempts lbRoutesUeqs:$lbRoutesUeqs lbRoutesPis:$lbRoutesPis lbRoutesRds:$lbRoutesRds lbRoutesBms:$lbRoutesBms lbRoutesFx:$lbRoutesFx"
export lbMaximumFailoverAttempts lbRoutesUeqs lbRoutesPis lbRoutesRds lbRoutesBms lbRoutesFx

export ufw_app_debug_response_enabled ufw_app_debug_stack_trace_enabled

# AES filter
app_aes_filter_enable=true
export app_aes_filter_enable

# EWF properties
sftp_local_directory=/prodlib/CEWMSTH/sftp/output/
sftp_host=LXEWFPSGV110
sftp_port=22
sftp_user=ftpcewth
sftp_retry_count=2
sftp_retry_backoff_period=1000
sftp_session_wait_time=1000
sftp_session_pool_size=10
archive_file_remote_directory=/CEWTH/EWFTH/input/
archive_file_workstation_ipaddress=LXCEWPSGV022
archive_file_customer_country=TH
ewf_request_archival_api_base_path=https://ewfth.sg.uobnet.com:1445/v11
ewf_signer_required=true
archive_file_jks_file_path=$conf_dir/fileuploadservice/jks.properties
archive_file_log_file_path=$conf_dir/fileuploadservice/log.properties
archive_file_sftp_temp_file=$log_dir/sftp_tmp_file
archive_file_permissions_file_path=$conf_dir/fileuploadservice/change_permission_sftp.sh

export ewf_signer_required archive_file_jks_file_path archive_file_log_file_path archive_file_sftp_temp_file archive_file_permissions_file_path
export sftp_local_directory sftp_host sftp_port sftp_user sftp_retry_count sftp_retry_backoff_period
export sftp_session_wait_time sftp_session_pool_size archive_file_remote_directory archive_file_workstation_ipaddress
export archive_file_customer_country ewf_request_archival_api_base_path
export account_filter_features_tobe_excluded=eTax-Payment-Report,Tax-Certificate-Report,Credit-Debit-Advice,View-Payroll-Amount,View-Payroll-Details
