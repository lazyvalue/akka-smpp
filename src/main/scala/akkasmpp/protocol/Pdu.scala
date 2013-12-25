package akkasmpp.protocol

/**
 * PDUs from SMPP 3.4
 */
import akka.util.{ByteString, ByteIterator}
import CommandId.CommandId
import CommandStatus.CommandStatus
import akkasmpp.protocol.TypeOfNumber.TypeOfNumber
import akkasmpp.protocol.NumericPlanIndicator.NumericPlanIndicator
import akkasmpp.protocol.ServiceType.ServiceType
import akkasmpp.protocol.Priority.Priority
import akkasmpp.protocol.RegisteredDelivery.RegisteredDelivery
import akkasmpp.protocol.DataCodingScheme.DataCodingScheme
import akkasmpp.protocol.EsmClass.EsmClass
import akkasmpp.protocol.MessageState.MessageState
import akkasmpp.protocol.bytestrings.WritePdu
import akkasmpp.protocol.bytestrings.WritePdu.DataSmRespWriter


object Pdu {
  implicit val byteOrder = java.nio.ByteOrder.BIG_ENDIAN
  def fromBytes(bytes: ByteIterator): Pdu = {
    val len = bytes.getInt
    val cmdId = CommandId(bytes.getInt)
    val cmdStatus = CommandStatus(bytes.getInt)
    val seqNum = bytes.getInt
    new Pdu(cmdId) {
      val commandLength: SmppTypes.Integer = len
      val sequenceNumber: SmppTypes.Integer = seqNum
      val commandStatus = cmdStatus
      def toByteString: ByteString = ???
    }
  }
}

abstract class Pdu(val commandId: CommandId) {
  override def toString() = {
    s"Pdu($commandLength, $commandId, $commandStatus, $sequenceNumber)"
  }

  def commandLength: SmppTypes.Integer
  def sequenceNumber: SmppTypes.Integer
  def toByteString: ByteString
  def commandStatus: CommandStatus
}

trait NullCommandStatus {
  def commandStatus = CommandStatus.ESME_ROK
}

trait BindLike extends Pdu with NullCommandStatus with WritePdu.BindWriter {
  def systemId: String
  def password: String
  def systemType: Option[String]
  def interfaceVersion: Byte
  def addrTon: TypeOfNumber
  def addrNpi: NumericPlanIndicator
}

trait BindRespLike extends Pdu with WritePdu.BindRespWriter {
  def systemId: Option[SmppTypes.COctetString]
  def scInterfaceVersion: Option[Tlv]
}

// deliver_sm / submit_sm. But not submit_multi or data_sm
trait SmLike extends Pdu with WritePdu.SmWriter {
  def commandLength: SmppTypes.Integer
  def commandStatus: CommandStatus
  def sequenceNumber: SmppTypes.Integer
  def serviceType: ServiceType
  def sourceAddrTon: TypeOfNumber
  def sourceAddrNpi: NumericPlanIndicator
  def sourceAddr: SmppTypes.COctetString
  def destAddrTon: TypeOfNumber
  def destAddrNpi: NumericPlanIndicator
  def destinationAddr: SmppTypes.COctetString
  def esmClass: EsmClass
  def protocolId: Byte
  def priorityFlag: Priority
  def scheduleDeliveryTime: TimeFormat
  def validityPeriod: TimeFormat
  def registeredDelivery: RegisteredDelivery
  def replaceIfPresentFlag: Boolean
  def dataCoding: DataCodingScheme
  def smDefaultMsgId: Byte
  def smLength: SmppTypes.Integer
  def shortMessage: SmppTypes.COctetString
  def tlvs: List[Tlv]
}

trait SmRespLike extends Pdu with WritePdu.SmRespWriter {
  def messageId: SmppTypes.COctetString
}

/**
 * Binds in transmit only mode
 */
case class BindTransmitter(commandLength: SmppTypes.Integer, sequenceNumber: SmppTypes.Integer, systemId: String, password: String,
                           systemType: Option[String], interfaceVersion: Byte, addrTon: TypeOfNumber, addrNpi: NumericPlanIndicator)
  extends Pdu(CommandId.bind_transmitter) with BindLike

case class BindTransmitterResp(commandLength: SmppTypes.Integer, commandStatus: CommandStatus, sequenceNumber: SmppTypes.Integer,
                               systemId: Option[SmppTypes.COctetString], scInterfaceVersion: Option[Tlv])
  extends Pdu(CommandId.bind_transmitter_resp) with BindRespLike

/**
 * Binds in receive only mode
 */
case class BindReceiver(commandLength: SmppTypes.Integer, sequenceNumber: SmppTypes.Integer, systemId: String, password: String,
                        systemType: Option[String], interfaceVersion: Byte, addrTon: TypeOfNumber, addrNpi: NumericPlanIndicator)
  extends Pdu(CommandId.bind_receiver) with BindLike

case class BindReceiverResp(commandLength: SmppTypes.Integer, commandStatus: CommandStatus, sequenceNumber: SmppTypes.Integer,
                               systemId: Option[SmppTypes.COctetString], scInterfaceVersion: Option[Tlv])
  extends Pdu(CommandId.bind_receiver_resp) with BindRespLike

/**
 * Binds in transmit and receiver mode
 */
case class BindTransceiver(commandLength: SmppTypes.Integer, sequenceNumber: SmppTypes.Integer, systemId: String, password: String,
                        systemType: Option[String], interfaceVersion: Byte, addrTon: TypeOfNumber, addrNpi: NumericPlanIndicator)
  extends Pdu(CommandId.bind_transceiver) with BindLike

case class BindTransceiverResp(commandLength: SmppTypes.Integer, commandStatus: CommandStatus, sequenceNumber: SmppTypes.Integer,
                            systemId: Option[SmppTypes.COctetString], scInterfaceVersion: Option[Tlv])
  extends Pdu(CommandId.bind_transceiver_resp) with BindRespLike

/**
 * For SMSC to initiate the bind (instead of ESME). Unlikely to be supported in this library
 */
case class Outbind(commandLength: SmppTypes.Integer, sequenceNumber: SmppTypes.Integer, systemId: SmppTypes.COctetString,
                   password: SmppTypes.COctetString) extends Pdu(CommandId.outbind) with NullCommandStatus with WritePdu.OutbindWriter

/**
  Tears down the connection
 */
case class Unbind(commandLength: SmppTypes.Integer, sequenceNumber: SmppTypes.Integer)
  extends Pdu(CommandId.unbind) with NullCommandStatus with WritePdu.HeaderOnlyWriter

case class UnbindResp(commandLength: SmppTypes.Integer, commandStatus: CommandStatus, sequenceNumber: SmppTypes.Integer)
  extends Pdu(CommandId.unbind) with WritePdu.HeaderOnlyWriter

/**
 * Response to an invalid PDU
 */

case class GenericNack(commandLength: SmppTypes.Integer, commandStatus: CommandStatus, sequenceNumber: SmppTypes.Integer)
  extends Pdu(CommandId.generic_nack) with WritePdu.HeaderOnlyWriter


/**
 * Submits a short message from the ESME to the SMSC
 */
case class SubmitSm(
                     commandLength: SmppTypes.Integer,
                     commandStatus: CommandStatus,
                     sequenceNumber: SmppTypes.Integer,
                     serviceType: ServiceType,
                     sourceAddrTon: TypeOfNumber,
                     sourceAddrNpi: NumericPlanIndicator,
                     sourceAddr: SmppTypes.COctetString,
                     destAddrTon: TypeOfNumber,
                     destAddrNpi: NumericPlanIndicator,
                     destinationAddr: SmppTypes.COctetString,
                     esmClass: EsmClass,
                     protocolId: Byte,
                     priorityFlag: Priority,
                     scheduleDeliveryTime: TimeFormat,
                     validityPeriod: TimeFormat,
                     registeredDelivery: RegisteredDelivery,
                     replaceIfPresentFlag: Boolean,
                     dataCoding: DataCodingScheme,
                     smDefaultMsgId: Byte,
                     smLength: SmppTypes.Integer,
                     shortMessage: SmppTypes.COctetString,
                     tlvs: List[Tlv]
                     ) extends Pdu(CommandId.submit_sm) with SmLike

case class SubmitSmResp(commandLength: SmppTypes.Integer, commandStatus: CommandStatus, sequenceNumber: SmppTypes.Integer, messageId: SmppTypes.COctetString)
  extends Pdu(CommandId.submit_sm_resp) with SmRespLike

case class SubmitMulti(
                        commandLength: SmppTypes.Integer,
                        commandStatus: CommandStatus,
                        sequenceNumber: SmppTypes.Integer,
                        serviceType: ServiceType,
                        sourceAddrTon: TypeOfNumber,
                        sourceAddrNpi: NumericPlanIndicator,
                        sourceAddr: SmppTypes.COctetString,
                        numberOfDests: SmppTypes.Integer,
                        destAddresses: List[(TypeOfNumber, NumericPlanIndicator, SmppTypes.COctetString)],
                        esmClass: EsmClass,
                        protocolId: Byte,
                        priorityFlag: Priority,
                        scheduleDeliveryTime: TimeFormat,
                        validityPeriod: TimeFormat,
                        registeredDelivery: RegisteredDelivery,
                        replaceIfPresentFlag: Boolean,
                        dataCoding: DataCodingScheme,
                        smDefaultMsgId: Byte,
                        smLength: SmppTypes.Integer,
                        shortMessage: SmppTypes.OctetString,
                        tlvs: List[Tlv]
                        ) extends Pdu(CommandId.submit_multi) with WritePdu.SubmitMultiWriter

case class SubmitMultiResp(commandLength: SmppTypes.Integer, commandStatus: CommandStatus, sequenceNumber: SmppTypes.Integer,
                            messageId: SmppTypes.COctetString, noUnsuccess: Byte,
                            unsuccessSmes: List[(TypeOfNumber, NumericPlanIndicator, SmppTypes.COctetString, CommandStatus)])
  extends Pdu(CommandId.submit_sm_resp) with WritePdu.SubmitMultiRespWriter

case class DeliverSm(
                     commandLength: SmppTypes.Integer,
                     commandStatus: CommandStatus,
                     sequenceNumber: SmppTypes.Integer,
                     serviceType: ServiceType,
                     sourceAddrTon: TypeOfNumber,
                     sourceAddrNpi: NumericPlanIndicator,
                     sourceAddr: SmppTypes.COctetString,
                     destAddrTon: TypeOfNumber,
                     destAddrNpi: NumericPlanIndicator,
                     destinationAddr: SmppTypes.COctetString,
                     esmClass: EsmClass,
                     protocolId: Byte,
                     priorityFlag: Priority,
                     scheduleDeliveryTime: TimeFormat,
                     validityPeriod: TimeFormat,
                     registeredDelivery: RegisteredDelivery,
                     replaceIfPresentFlag: Boolean,
                     dataCoding: DataCodingScheme,
                     smDefaultMsgId: Byte,
                     smLength: SmppTypes.Integer,
                     shortMessage: SmppTypes.OctetString,
                     tlvs: List[Tlv]
                     ) extends Pdu(CommandId.submit_sm) with SmLike

case class DeliverSmResp(commandLength: SmppTypes.Integer, commandStatus: CommandStatus, sequenceNumber: SmppTypes.Integer, messageId: SmppTypes.COctetString)
  extends Pdu(CommandId.submit_sm_resp) with SmRespLike

case class DataSm(commandLength: SmppTypes.Integer, sequenceNumber: SmppTypes.Integer, serviceType: ServiceType,
                  sourceAddrTon: TypeOfNumber, sourceAddrNpi: NumericPlanIndicator, sourceAddr: SmppTypes.COctetString,
                  destAddrTon: TypeOfNumber, destAddrNpi: NumericPlanIndicator, destinationAddr: SmppTypes.COctetString,
                  esmClass: EsmClass, registeredDelivery: RegisteredDelivery, dataCoding: DataCodingScheme, tlvs: List[Tlv])
  extends Pdu(CommandId.data_sm) with NullCommandStatus with WritePdu.DataSmWriter

case class DataSmResp(commandLength: SmppTypes.Integer, commandStatus: CommandStatus, sequenceNumber: SmppTypes.Integer, messageId: SmppTypes.COctetString,
                      tlvs: List[Tlv]) extends Pdu(CommandId.data_sm_resp) with DataSmRespWriter

case class QuerySm(commandLength: SmppTypes.Integer, sequenceNumber: SmppTypes.Integer, messageId: SmppTypes.COctetString,
                   sourceAddrTon: TypeOfNumber, sourceAddrNpi: NumericPlanIndicator, sourceAddr: SmppTypes.COctetString)
  extends Pdu(CommandId.query_sm) with NullCommandStatus with WritePdu.QuerySmWriter

case class QuerySmResp(commandLength: SmppTypes.Integer, commandStatus: CommandStatus, sequenceNumber: SmppTypes.Integer,
                       messageId: SmppTypes.COctetString, finalDate: TimeFormat, messageState: MessageState, errorCode: SmppTypes.Integer)
  extends Pdu(CommandId.query_sm_resp) with WritePdu.QuerySmRespWriter

case class CancelSm(commandLength: SmppTypes.Integer, sequenceNumber: SmppTypes.Integer, serviceType: ServiceType,
                    messageId: SmppTypes.COctetString, sourceAddrTon : TypeOfNumber, sourceAddrNpi: NumericPlanIndicator,
                    sourceAddr: SmppTypes.COctetString, destAddrTon: TypeOfNumber, destAddrNpi: NumericPlanIndicator,
                    destinationAddr: SmppTypes.COctetString)
  extends Pdu(CommandId.cancel_sm) with NullCommandStatus with WritePdu.CancelSmWriter

case class CancelSmResp(commandLength: SmppTypes.Integer, commandStatus: CommandStatus, sequenceNumber: SmppTypes.Integer)
  extends Pdu(CommandId.cancel_sm_resp) with WritePdu.HeaderOnlyWriter

case class ReplaceSm(commandLength: SmppTypes.Integer, sequenceNumber: SmppTypes.Integer, messageId: SmppTypes.COctetString,
                     sourceAddrTon: TypeOfNumber, sourceAddrNpi: NumericPlanIndicator, sourceAddr: SmppTypes.COctetString,
                     scheduleDeliveryTime: TimeFormat, validityPeriod: TimeFormat, registeredDelivery: RegisteredDelivery,
                     smDefaultMsgId: Byte, smLength: SmppTypes.Integer, shortMessage: SmppTypes.OctetString)
  extends Pdu(CommandId.replace_sm) with NullCommandStatus with WritePdu.ReplaceSmWriter

case class ReplaceSmResp(commandLength: SmppTypes.Integer, commandStatus: CommandStatus, sequenceNumber: SmppTypes.Integer)
  extends Pdu(CommandId.replace_sm_resp) with WritePdu.HeaderOnlyWriter

case class EnquireLink(commandLength: SmppTypes.Integer, sequenceNumber: SmppTypes.Integer)
  extends Pdu(CommandId.enquire_link) with NullCommandStatus with WritePdu.HeaderOnlyWriter

case class EnquireLinkResp(commandLength: SmppTypes.Integer, sequenceNumber: SmppTypes.Integer)
  extends Pdu(CommandId.enquire_link_resp) with NullCommandStatus with WritePdu.HeaderOnlyWriter

case class AlertNotification(commandLength: SmppTypes.Integer, sequenceNumber: SmppTypes.Integer,
                             sourceAddrTon: TypeOfNumber, sourceAddrNpi: NumericPlanIndicator,
                             sourceAddr: SmppTypes.COctetString,  esmeAddrTon: TypeOfNumber,
                             esmeAddrNpi: NumericPlanIndicator, esmeAddr: SmppTypes.COctetString,
                             msAvailabilityStatus: Option[Tlv]) extends Pdu(CommandId.alert_notification) with NullCommandStatus
                             with WritePdu.AlertNotificationWriter