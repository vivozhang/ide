/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 2.0.4
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package org.zaluum.visa.swig;

public class JVisaJNI {
  public final static native int viOpenDefaultRM(long[] jarg1);
  public final static native int viFindRsrc(long jarg1, String jarg2, long jarg3, long jarg4, String jarg5);
  public final static native int viFindNext(long jarg1, String jarg2);
  public final static native int viParseRsrc(long jarg1, String jarg2, long jarg3, long jarg4);
  public final static native int viParseRsrcEx(long jarg1, String jarg2, long jarg3, long jarg4, String jarg5, String jarg6, String jarg7);
  public final static native int viOpen(long jarg1, String jarg2, long jarg3, long jarg4, long[] jarg5);
  public final static native int viClose(long jarg1);
  public final static native int viSetAttribute(long jarg1, long jarg2, long jarg3);
  public final static native int viGetAttribute(long jarg1, long jarg2, long jarg3);
  public final static native int viStatusDesc(long jarg1, int jarg2, String jarg3);
  public final static native int viTerminate(long jarg1, int jarg2, long jarg3);
  public final static native int viLock(long jarg1, long jarg2, long jarg3, String jarg4, String jarg5);
  public final static native int viUnlock(long jarg1);
  public final static native int viEnableEvent(long jarg1, long jarg2, int jarg3, long jarg4);
  public final static native int viDisableEvent(long jarg1, long jarg2, int jarg3);
  public final static native int viDiscardEvents(long jarg1, long jarg2, int jarg3);
  public final static native int viWaitOnEvent(long jarg1, long jarg2, long jarg3, long jarg4, long jarg5);
  public final static native int viInstallHandler(long jarg1, long jarg2, long jarg3, long jarg4);
  public final static native int viUninstallHandler(long jarg1, long jarg2, long jarg3, long jarg4);
  public final static native int viRead(long jarg1, long jarg2, long jarg3, long jarg4);
  public final static native int viReadAsync(long jarg1, long jarg2, long jarg3, long jarg4);
  public final static native int viReadToFile(long jarg1, String jarg2, long jarg3, long jarg4);
  public final static native int viWrite(long jarg1, long jarg2, long jarg3, long jarg4);
  public final static native int viWriteAsync(long jarg1, long jarg2, long jarg3, long jarg4);
  public final static native int viWriteFromFile(long jarg1, String jarg2, long jarg3, long jarg4);
  public final static native int viAssertTrigger(long jarg1, int jarg2);
  public final static native int viReadSTB(long jarg1, long jarg2);
  public final static native int viClear(long jarg1);
  public final static native int viSetBuf(long jarg1, int jarg2, long jarg3);
  public final static native int viFlush(long jarg1, int jarg2);
  public final static native int viBufWrite(long jarg1, long jarg2, long jarg3, long jarg4);
  public final static native int viBufRead(long jarg1, long jarg2, long jarg3, long jarg4);
  public final static native int viPrintf(long jarg1, String jarg2);
  public final static native int viSPrintf(long jarg1, long jarg2, String jarg3);
  public final static native int viScanf(long jarg1, String jarg2);
  public final static native int viSScanf(long jarg1, long jarg2, String jarg3);
  public final static native int viQueryf(long jarg1, String jarg2, String jarg3);
  public final static native int viIn8(long jarg1, int jarg2, long jarg3, long jarg4);
  public final static native int viOut8(long jarg1, int jarg2, long jarg3, short jarg4);
  public final static native int viIn16(long jarg1, int jarg2, long jarg3, long jarg4);
  public final static native int viOut16(long jarg1, int jarg2, long jarg3, int jarg4);
  public final static native int viIn32(long jarg1, int jarg2, long jarg3, long jarg4);
  public final static native int viOut32(long jarg1, int jarg2, long jarg3, long jarg4);
  public final static native int viMoveIn8(long jarg1, int jarg2, long jarg3, long jarg4, long jarg5);
  public final static native int viMoveOut8(long jarg1, int jarg2, long jarg3, long jarg4, long jarg5);
  public final static native int viMoveIn16(long jarg1, int jarg2, long jarg3, long jarg4, long jarg5);
  public final static native int viMoveOut16(long jarg1, int jarg2, long jarg3, long jarg4, long jarg5);
  public final static native int viMoveIn32(long jarg1, int jarg2, long jarg3, long jarg4, long jarg5);
  public final static native int viMoveOut32(long jarg1, int jarg2, long jarg3, long jarg4, long jarg5);
  public final static native int viMove(long jarg1, int jarg2, long jarg3, int jarg4, int jarg5, long jarg6, int jarg7, long jarg8);
  public final static native int viMoveAsync(long jarg1, int jarg2, long jarg3, int jarg4, int jarg5, long jarg6, int jarg7, long jarg8, long jarg9);
  public final static native int viMapAddress(long jarg1, int jarg2, long jarg3, long jarg4, int jarg5, long jarg6, long jarg7);
  public final static native int viUnmapAddress(long jarg1);
  public final static native void viPeek8(long jarg1, long jarg2, long jarg3);
  public final static native void viPoke8(long jarg1, long jarg2, short jarg3);
  public final static native void viPeek16(long jarg1, long jarg2, long jarg3);
  public final static native void viPoke16(long jarg1, long jarg2, int jarg3);
  public final static native void viPeek32(long jarg1, long jarg2, long jarg3);
  public final static native void viPoke32(long jarg1, long jarg2, long jarg3);
  public final static native int viMemAlloc(long jarg1, long jarg2, long jarg3);
  public final static native int viMemFree(long jarg1, long jarg2);
  public final static native int viGpibControlREN(long jarg1, int jarg2);
  public final static native int viGpibControlATN(long jarg1, int jarg2);
  public final static native int viGpibSendIFC(long jarg1);
  public final static native int viGpibCommand(long jarg1, long jarg2, long jarg3, long jarg4);
  public final static native int viGpibPassControl(long jarg1, int jarg2, int jarg3);
  public final static native int viVxiCommandQuery(long jarg1, int jarg2, long jarg3, long jarg4);
  public final static native int viAssertUtilSignal(long jarg1, int jarg2);
  public final static native int viAssertIntrSignal(long jarg1, short jarg2, long jarg3);
  public final static native int viMapTrigger(long jarg1, short jarg2, short jarg3, int jarg4);
  public final static native int viUnmapTrigger(long jarg1, short jarg2, short jarg3);
  public final static native int viUsbControlOut(long jarg1, short jarg2, short jarg3, int jarg4, int jarg5, int jarg6, long jarg7);
  public final static native int viUsbControlIn(long jarg1, short jarg2, short jarg3, int jarg4, int jarg5, int jarg6, long jarg7, long jarg8);
  public final static native int viVxiServantResponse(long jarg1, short jarg2, long jarg3);
  public final static native long VI_ATTR_RSRC_CLASS_get();
  public final static native long VI_ATTR_RSRC_NAME_get();
  public final static native long VI_ATTR_RSRC_IMPL_VERSION_get();
  public final static native long VI_ATTR_RSRC_LOCK_STATE_get();
  public final static native long VI_ATTR_MAX_QUEUE_LENGTH_get();
  public final static native long VI_ATTR_USER_DATA_32_get();
  public final static native long VI_ATTR_FDC_CHNL_get();
  public final static native long VI_ATTR_FDC_MODE_get();
  public final static native long VI_ATTR_FDC_GEN_SIGNAL_EN_get();
  public final static native long VI_ATTR_FDC_USE_PAIR_get();
  public final static native long VI_ATTR_SEND_END_EN_get();
  public final static native long VI_ATTR_TERMCHAR_get();
  public final static native long VI_ATTR_TMO_VALUE_get();
  public final static native long VI_ATTR_GPIB_READDR_EN_get();
  public final static native long VI_ATTR_IO_PROT_get();
  public final static native long VI_ATTR_DMA_ALLOW_EN_get();
  public final static native long VI_ATTR_ASRL_BAUD_get();
  public final static native long VI_ATTR_ASRL_DATA_BITS_get();
  public final static native long VI_ATTR_ASRL_PARITY_get();
  public final static native long VI_ATTR_ASRL_STOP_BITS_get();
  public final static native long VI_ATTR_ASRL_FLOW_CNTRL_get();
  public final static native long VI_ATTR_RD_BUF_OPER_MODE_get();
  public final static native long VI_ATTR_RD_BUF_SIZE_get();
  public final static native long VI_ATTR_WR_BUF_OPER_MODE_get();
  public final static native long VI_ATTR_WR_BUF_SIZE_get();
  public final static native long VI_ATTR_SUPPRESS_END_EN_get();
  public final static native long VI_ATTR_TERMCHAR_EN_get();
  public final static native long VI_ATTR_DEST_ACCESS_PRIV_get();
  public final static native long VI_ATTR_DEST_BYTE_ORDER_get();
  public final static native long VI_ATTR_SRC_ACCESS_PRIV_get();
  public final static native long VI_ATTR_SRC_BYTE_ORDER_get();
  public final static native long VI_ATTR_SRC_INCREMENT_get();
  public final static native long VI_ATTR_DEST_INCREMENT_get();
  public final static native long VI_ATTR_WIN_ACCESS_PRIV_get();
  public final static native long VI_ATTR_WIN_BYTE_ORDER_get();
  public final static native long VI_ATTR_GPIB_ATN_STATE_get();
  public final static native long VI_ATTR_GPIB_ADDR_STATE_get();
  public final static native long VI_ATTR_GPIB_CIC_STATE_get();
  public final static native long VI_ATTR_GPIB_NDAC_STATE_get();
  public final static native long VI_ATTR_GPIB_SRQ_STATE_get();
  public final static native long VI_ATTR_GPIB_SYS_CNTRL_STATE_get();
  public final static native long VI_ATTR_GPIB_HS488_CBL_LEN_get();
  public final static native long VI_ATTR_CMDR_LA_get();
  public final static native long VI_ATTR_VXI_DEV_CLASS_get();
  public final static native long VI_ATTR_MAINFRAME_LA_get();
  public final static native long VI_ATTR_MANF_NAME_get();
  public final static native long VI_ATTR_MODEL_NAME_get();
  public final static native long VI_ATTR_VXI_VME_INTR_STATUS_get();
  public final static native long VI_ATTR_VXI_TRIG_STATUS_get();
  public final static native long VI_ATTR_VXI_VME_SYSFAIL_STATE_get();
  public final static native long VI_ATTR_WIN_BASE_ADDR_32_get();
  public final static native long VI_ATTR_WIN_SIZE_32_get();
  public final static native long VI_ATTR_ASRL_AVAIL_NUM_get();
  public final static native long VI_ATTR_MEM_BASE_32_get();
  public final static native long VI_ATTR_ASRL_CTS_STATE_get();
  public final static native long VI_ATTR_ASRL_DCD_STATE_get();
  public final static native long VI_ATTR_ASRL_DSR_STATE_get();
  public final static native long VI_ATTR_ASRL_DTR_STATE_get();
  public final static native long VI_ATTR_ASRL_END_IN_get();
  public final static native long VI_ATTR_ASRL_END_OUT_get();
  public final static native long VI_ATTR_ASRL_REPLACE_CHAR_get();
  public final static native long VI_ATTR_ASRL_RI_STATE_get();
  public final static native long VI_ATTR_ASRL_RTS_STATE_get();
  public final static native long VI_ATTR_ASRL_XON_CHAR_get();
  public final static native long VI_ATTR_ASRL_XOFF_CHAR_get();
  public final static native long VI_ATTR_WIN_ACCESS_get();
  public final static native long VI_ATTR_RM_SESSION_get();
  public final static native long VI_ATTR_VXI_LA_get();
  public final static native long VI_ATTR_MANF_ID_get();
  public final static native long VI_ATTR_MEM_SIZE_32_get();
  public final static native long VI_ATTR_MEM_SPACE_get();
  public final static native long VI_ATTR_MODEL_CODE_get();
  public final static native long VI_ATTR_SLOT_get();
  public final static native long VI_ATTR_INTF_INST_NAME_get();
  public final static native long VI_ATTR_IMMEDIATE_SERV_get();
  public final static native long VI_ATTR_INTF_PARENT_NUM_get();
  public final static native long VI_ATTR_RSRC_SPEC_VERSION_get();
  public final static native long VI_ATTR_INTF_TYPE_get();
  public final static native long VI_ATTR_GPIB_PRIMARY_ADDR_get();
  public final static native long VI_ATTR_GPIB_SECONDARY_ADDR_get();
  public final static native long VI_ATTR_RSRC_MANF_NAME_get();
  public final static native long VI_ATTR_RSRC_MANF_ID_get();
  public final static native long VI_ATTR_INTF_NUM_get();
  public final static native long VI_ATTR_TRIG_ID_get();
  public final static native long VI_ATTR_GPIB_REN_STATE_get();
  public final static native long VI_ATTR_GPIB_UNADDR_EN_get();
  public final static native long VI_ATTR_DEV_STATUS_BYTE_get();
  public final static native long VI_ATTR_FILE_APPEND_EN_get();
  public final static native long VI_ATTR_VXI_TRIG_SUPPORT_get();
  public final static native long VI_ATTR_TCPIP_ADDR_get();
  public final static native long VI_ATTR_TCPIP_HOSTNAME_get();
  public final static native long VI_ATTR_TCPIP_PORT_get();
  public final static native long VI_ATTR_TCPIP_DEVICE_NAME_get();
  public final static native long VI_ATTR_TCPIP_NODELAY_get();
  public final static native long VI_ATTR_TCPIP_KEEPALIVE_get();
  public final static native long VI_ATTR_4882_COMPLIANT_get();
  public final static native long VI_ATTR_USB_SERIAL_NUM_get();
  public final static native long VI_ATTR_USB_INTFC_NUM_get();
  public final static native long VI_ATTR_USB_PROTOCOL_get();
  public final static native long VI_ATTR_USB_MAX_INTR_SIZE_get();
  public final static native long VI_ATTR_PXI_DEV_NUM_get();
  public final static native long VI_ATTR_PXI_FUNC_NUM_get();
  public final static native long VI_ATTR_PXI_BUS_NUM_get();
  public final static native long VI_ATTR_PXI_CHASSIS_get();
  public final static native long VI_ATTR_PXI_SLOTPATH_get();
  public final static native long VI_ATTR_PXI_SLOT_LBUS_LEFT_get();
  public final static native long VI_ATTR_PXI_SLOT_LBUS_RIGHT_get();
  public final static native long VI_ATTR_PXI_TRIG_BUS_get();
  public final static native long VI_ATTR_PXI_STAR_TRIG_BUS_get();
  public final static native long VI_ATTR_PXI_STAR_TRIG_LINE_get();
  public final static native long VI_ATTR_PXI_MEM_TYPE_BAR0_get();
  public final static native long VI_ATTR_PXI_MEM_TYPE_BAR1_get();
  public final static native long VI_ATTR_PXI_MEM_TYPE_BAR2_get();
  public final static native long VI_ATTR_PXI_MEM_TYPE_BAR3_get();
  public final static native long VI_ATTR_PXI_MEM_TYPE_BAR4_get();
  public final static native long VI_ATTR_PXI_MEM_TYPE_BAR5_get();
  public final static native long VI_ATTR_PXI_MEM_BASE_BAR0_get();
  public final static native long VI_ATTR_PXI_MEM_BASE_BAR1_get();
  public final static native long VI_ATTR_PXI_MEM_BASE_BAR2_get();
  public final static native long VI_ATTR_PXI_MEM_BASE_BAR3_get();
  public final static native long VI_ATTR_PXI_MEM_BASE_BAR4_get();
  public final static native long VI_ATTR_PXI_MEM_BASE_BAR5_get();
  public final static native long VI_ATTR_PXI_MEM_SIZE_BAR0_get();
  public final static native long VI_ATTR_PXI_MEM_SIZE_BAR1_get();
  public final static native long VI_ATTR_PXI_MEM_SIZE_BAR2_get();
  public final static native long VI_ATTR_PXI_MEM_SIZE_BAR3_get();
  public final static native long VI_ATTR_PXI_MEM_SIZE_BAR4_get();
  public final static native long VI_ATTR_PXI_MEM_SIZE_BAR5_get();
  public final static native long VI_ATTR_PXI_IS_EXPRESS_get();
  public final static native long VI_ATTR_PXI_SLOT_LWIDTH_get();
  public final static native long VI_ATTR_PXI_MAX_LWIDTH_get();
  public final static native long VI_ATTR_PXI_ACTUAL_LWIDTH_get();
  public final static native long VI_ATTR_PXI_DSTAR_BUS_get();
  public final static native long VI_ATTR_PXI_DSTAR_SET_get();
  public final static native long VI_ATTR_TCPIP_HISLIP_OVERLAP_EN_get();
  public final static native long VI_ATTR_TCPIP_HISLIP_VERSION_get();
  public final static native long VI_ATTR_TCPIP_HISLIP_MAX_MESSAGE_KB_get();
  public final static native long VI_ATTR_JOB_ID_get();
  public final static native long VI_ATTR_EVENT_TYPE_get();
  public final static native long VI_ATTR_SIGP_STATUS_ID_get();
  public final static native long VI_ATTR_RECV_TRIG_ID_get();
  public final static native long VI_ATTR_INTR_STATUS_ID_get();
  public final static native long VI_ATTR_STATUS_get();
  public final static native long VI_ATTR_RET_COUNT_32_get();
  public final static native long VI_ATTR_BUFFER_get();
  public final static native long VI_ATTR_RECV_INTR_LEVEL_get();
  public final static native long VI_ATTR_OPER_NAME_get();
  public final static native long VI_ATTR_GPIB_RECV_CIC_STATE_get();
  public final static native long VI_ATTR_RECV_TCPIP_ADDR_get();
  public final static native long VI_ATTR_USB_RECV_INTR_SIZE_get();
  public final static native long VI_ATTR_USB_RECV_INTR_DATA_get();
  public final static native long VI_ATTR_USER_DATA_get();
  public final static native long VI_ATTR_RET_COUNT_get();
  public final static native long VI_ATTR_WIN_BASE_ADDR_get();
  public final static native long VI_ATTR_WIN_SIZE_get();
  public final static native long VI_ATTR_MEM_BASE_get();
  public final static native long VI_ATTR_MEM_SIZE_get();
  public final static native long VI_EVENT_IO_COMPLETION_get();
  public final static native long VI_EVENT_TRIG_get();
  public final static native long VI_EVENT_SERVICE_REQ_get();
  public final static native long VI_EVENT_CLEAR_get();
  public final static native long VI_EVENT_EXCEPTION_get();
  public final static native long VI_EVENT_GPIB_CIC_get();
  public final static native long VI_EVENT_GPIB_TALK_get();
  public final static native long VI_EVENT_GPIB_LISTEN_get();
  public final static native long VI_EVENT_VXI_VME_SYSFAIL_get();
  public final static native long VI_EVENT_VXI_VME_SYSRESET_get();
  public final static native long VI_EVENT_VXI_SIGP_get();
  public final static native long VI_EVENT_VXI_VME_INTR_get();
  public final static native long VI_EVENT_PXI_INTR_get();
  public final static native long VI_EVENT_TCPIP_CONNECT_get();
  public final static native long VI_EVENT_USB_INTR_get();
  public final static native long VI_ALL_ENABLED_EVENTS_get();
  public final static native int VI_SUCCESS_EVENT_EN_get();
  public final static native int VI_SUCCESS_EVENT_DIS_get();
  public final static native int VI_SUCCESS_QUEUE_EMPTY_get();
  public final static native int VI_SUCCESS_TERM_CHAR_get();
  public final static native int VI_SUCCESS_MAX_CNT_get();
  public final static native int VI_SUCCESS_DEV_NPRESENT_get();
  public final static native int VI_SUCCESS_TRIG_MAPPED_get();
  public final static native int VI_SUCCESS_QUEUE_NEMPTY_get();
  public final static native int VI_SUCCESS_NCHAIN_get();
  public final static native int VI_SUCCESS_NESTED_SHARED_get();
  public final static native int VI_SUCCESS_NESTED_EXCLUSIVE_get();
  public final static native int VI_SUCCESS_SYNC_get();
  public final static native int VI_WARN_QUEUE_OVERFLOW_get();
  public final static native int VI_WARN_CONFIG_NLOADED_get();
  public final static native int VI_WARN_NULL_OBJECT_get();
  public final static native int VI_WARN_NSUP_ATTR_STATE_get();
  public final static native int VI_WARN_UNKNOWN_STATUS_get();
  public final static native int VI_WARN_NSUP_BUF_get();
  public final static native int VI_WARN_EXT_FUNC_NIMPL_get();
  public final static native int VI_FIND_BUFLEN_get();
  public final static native int VI_INTF_GPIB_get();
  public final static native int VI_INTF_VXI_get();
  public final static native int VI_INTF_GPIB_VXI_get();
  public final static native int VI_INTF_ASRL_get();
  public final static native int VI_INTF_PXI_get();
  public final static native int VI_INTF_TCPIP_get();
  public final static native int VI_INTF_USB_get();
  public final static native int VI_PROT_NORMAL_get();
  public final static native int VI_PROT_FDC_get();
  public final static native int VI_PROT_HS488_get();
  public final static native int VI_PROT_4882_STRS_get();
  public final static native int VI_PROT_USBTMC_VENDOR_get();
  public final static native int VI_FDC_NORMAL_get();
  public final static native int VI_FDC_STREAM_get();
  public final static native int VI_LOCAL_SPACE_get();
  public final static native int VI_A16_SPACE_get();
  public final static native int VI_A24_SPACE_get();
  public final static native int VI_A32_SPACE_get();
  public final static native int VI_A64_SPACE_get();
  public final static native int VI_PXI_ALLOC_SPACE_get();
  public final static native int VI_PXI_CFG_SPACE_get();
  public final static native int VI_PXI_BAR0_SPACE_get();
  public final static native int VI_PXI_BAR1_SPACE_get();
  public final static native int VI_PXI_BAR2_SPACE_get();
  public final static native int VI_PXI_BAR3_SPACE_get();
  public final static native int VI_PXI_BAR4_SPACE_get();
  public final static native int VI_PXI_BAR5_SPACE_get();
  public final static native int VI_OPAQUE_SPACE_get();
  public final static native int VI_UNKNOWN_LA_get();
  public final static native int VI_UNKNOWN_SLOT_get();
  public final static native int VI_UNKNOWN_LEVEL_get();
  public final static native int VI_UNKNOWN_CHASSIS_get();
  public final static native int VI_QUEUE_get();
  public final static native int VI_HNDLR_get();
  public final static native int VI_SUSPEND_HNDLR_get();
  public final static native int VI_ALL_MECH_get();
  public final static native int VI_ANY_HNDLR_get();
  public final static native int VI_TRIG_ALL_get();
  public final static native int VI_TRIG_SW_get();
  public final static native int VI_TRIG_TTL0_get();
  public final static native int VI_TRIG_TTL1_get();
  public final static native int VI_TRIG_TTL2_get();
  public final static native int VI_TRIG_TTL3_get();
  public final static native int VI_TRIG_TTL4_get();
  public final static native int VI_TRIG_TTL5_get();
  public final static native int VI_TRIG_TTL6_get();
  public final static native int VI_TRIG_TTL7_get();
  public final static native int VI_TRIG_ECL0_get();
  public final static native int VI_TRIG_ECL1_get();
  public final static native int VI_TRIG_PANEL_IN_get();
  public final static native int VI_TRIG_PANEL_OUT_get();
  public final static native int VI_TRIG_PROT_DEFAULT_get();
  public final static native int VI_TRIG_PROT_ON_get();
  public final static native int VI_TRIG_PROT_OFF_get();
  public final static native int VI_TRIG_PROT_SYNC_get();
  public final static native int VI_TRIG_PROT_RESERVE_get();
  public final static native int VI_TRIG_PROT_UNRESERVE_get();
  public final static native int VI_READ_BUF_get();
  public final static native int VI_WRITE_BUF_get();
  public final static native int VI_READ_BUF_DISCARD_get();
  public final static native int VI_WRITE_BUF_DISCARD_get();
  public final static native int VI_IO_IN_BUF_get();
  public final static native int VI_IO_OUT_BUF_get();
  public final static native int VI_IO_IN_BUF_DISCARD_get();
  public final static native int VI_IO_OUT_BUF_DISCARD_get();
  public final static native int VI_FLUSH_ON_ACCESS_get();
  public final static native int VI_FLUSH_WHEN_FULL_get();
  public final static native int VI_FLUSH_DISABLE_get();
  public final static native int VI_NMAPPED_get();
  public final static native int VI_USE_OPERS_get();
  public final static native int VI_DEREF_ADDR_get();
  public final static native int VI_DEREF_ADDR_BYTE_SWAP_get();
  public final static native int VI_TMO_IMMEDIATE_get();
  public final static native long VI_TMO_INFINITE_get();
  public final static native int VI_NO_LOCK_get();
  public final static native int VI_EXCLUSIVE_LOCK_get();
  public final static native int VI_SHARED_LOCK_get();
  public final static native int VI_LOAD_CONFIG_get();
  public final static native int VI_NO_SEC_ADDR_get();
  public final static native int VI_ASRL_PAR_NONE_get();
  public final static native int VI_ASRL_PAR_ODD_get();
  public final static native int VI_ASRL_PAR_EVEN_get();
  public final static native int VI_ASRL_PAR_MARK_get();
  public final static native int VI_ASRL_PAR_SPACE_get();
  public final static native int VI_ASRL_STOP_ONE_get();
  public final static native int VI_ASRL_STOP_ONE5_get();
  public final static native int VI_ASRL_STOP_TWO_get();
  public final static native int VI_ASRL_FLOW_NONE_get();
  public final static native int VI_ASRL_FLOW_XON_XOFF_get();
  public final static native int VI_ASRL_FLOW_RTS_CTS_get();
  public final static native int VI_ASRL_FLOW_DTR_DSR_get();
  public final static native int VI_ASRL_END_NONE_get();
  public final static native int VI_ASRL_END_LAST_BIT_get();
  public final static native int VI_ASRL_END_TERMCHAR_get();
  public final static native int VI_ASRL_END_BREAK_get();
  public final static native int VI_STATE_ASSERTED_get();
  public final static native int VI_STATE_UNASSERTED_get();
  public final static native int VI_STATE_UNKNOWN_get();
  public final static native int VI_BIG_ENDIAN_get();
  public final static native int VI_LITTLE_ENDIAN_get();
  public final static native int VI_DATA_PRIV_get();
  public final static native int VI_DATA_NPRIV_get();
  public final static native int VI_PROG_PRIV_get();
  public final static native int VI_PROG_NPRIV_get();
  public final static native int VI_BLCK_PRIV_get();
  public final static native int VI_BLCK_NPRIV_get();
  public final static native int VI_D64_PRIV_get();
  public final static native int VI_D64_NPRIV_get();
  public final static native int VI_WIDTH_8_get();
  public final static native int VI_WIDTH_16_get();
  public final static native int VI_WIDTH_32_get();
  public final static native int VI_WIDTH_64_get();
  public final static native int VI_GPIB_REN_DEASSERT_get();
  public final static native int VI_GPIB_REN_ASSERT_get();
  public final static native int VI_GPIB_REN_DEASSERT_GTL_get();
  public final static native int VI_GPIB_REN_ASSERT_ADDRESS_get();
  public final static native int VI_GPIB_REN_ASSERT_LLO_get();
  public final static native int VI_GPIB_REN_ASSERT_ADDRESS_LLO_get();
  public final static native int VI_GPIB_REN_ADDRESS_GTL_get();
  public final static native int VI_GPIB_ATN_DEASSERT_get();
  public final static native int VI_GPIB_ATN_ASSERT_get();
  public final static native int VI_GPIB_ATN_DEASSERT_HANDSHAKE_get();
  public final static native int VI_GPIB_ATN_ASSERT_IMMEDIATE_get();
  public final static native int VI_GPIB_HS488_DISABLED_get();
  public final static native int VI_GPIB_HS488_NIMPL_get();
  public final static native int VI_GPIB_UNADDRESSED_get();
  public final static native int VI_GPIB_TALKER_get();
  public final static native int VI_GPIB_LISTENER_get();
  public final static native int VI_VXI_CMD16_get();
  public final static native int VI_VXI_CMD16_RESP16_get();
  public final static native int VI_VXI_RESP16_get();
  public final static native int VI_VXI_CMD32_get();
  public final static native int VI_VXI_CMD32_RESP16_get();
  public final static native int VI_VXI_CMD32_RESP32_get();
  public final static native int VI_VXI_RESP32_get();
  public final static native int VI_ASSERT_SIGNAL_get();
  public final static native int VI_ASSERT_USE_ASSIGNED_get();
  public final static native int VI_ASSERT_IRQ1_get();
  public final static native int VI_ASSERT_IRQ2_get();
  public final static native int VI_ASSERT_IRQ3_get();
  public final static native int VI_ASSERT_IRQ4_get();
  public final static native int VI_ASSERT_IRQ5_get();
  public final static native int VI_ASSERT_IRQ6_get();
  public final static native int VI_ASSERT_IRQ7_get();
  public final static native int VI_UTIL_ASSERT_SYSRESET_get();
  public final static native int VI_UTIL_ASSERT_SYSFAIL_get();
  public final static native int VI_UTIL_DEASSERT_SYSFAIL_get();
  public final static native int VI_VXI_CLASS_MEMORY_get();
  public final static native int VI_VXI_CLASS_EXTENDED_get();
  public final static native int VI_VXI_CLASS_MESSAGE_get();
  public final static native int VI_VXI_CLASS_REGISTER_get();
  public final static native int VI_VXI_CLASS_OTHER_get();
  public final static native int VI_PXI_ADDR_NONE_get();
  public final static native int VI_PXI_ADDR_MEM_get();
  public final static native int VI_PXI_ADDR_IO_get();
  public final static native int VI_PXI_ADDR_CFG_get();
  public final static native int VI_TRIG_UNKNOWN_get();
  public final static native int VI_PXI_LBUS_UNKNOWN_get();
  public final static native int VI_PXI_LBUS_NONE_get();
  public final static native int VI_PXI_LBUS_STAR_TRIG_BUS_0_get();
  public final static native int VI_PXI_LBUS_STAR_TRIG_BUS_1_get();
  public final static native int VI_PXI_LBUS_STAR_TRIG_BUS_2_get();
  public final static native int VI_PXI_LBUS_STAR_TRIG_BUS_3_get();
  public final static native int VI_PXI_LBUS_STAR_TRIG_BUS_4_get();
  public final static native int VI_PXI_LBUS_STAR_TRIG_BUS_5_get();
  public final static native int VI_PXI_LBUS_STAR_TRIG_BUS_6_get();
  public final static native int VI_PXI_LBUS_STAR_TRIG_BUS_7_get();
  public final static native int VI_PXI_LBUS_STAR_TRIG_BUS_8_get();
  public final static native int VI_PXI_LBUS_STAR_TRIG_BUS_9_get();
  public final static native int VI_PXI_STAR_TRIG_CONTROLLER_get();
}
