// src/pages/WhatsApp.jsx
import { useState } from 'react';
import {
  Card, Form, Input, Button, Select, Tag, Typography, Space,
  List, Badge, Divider, Row, Col, Alert
} from 'antd';
import {
  SendOutlined, MessageOutlined, WhatsAppOutlined, PlusOutlined, UserOutlined
} from '@ant-design/icons';
import toast from 'react-hot-toast';
import api from '../api/axios';

const { TextArea } = Input;
const { Title, Text } = Typography;

export default function WhatsApp() {
  const [recipients, setRecipients] = useState([]);
  const [phoneInput, setPhoneInput] = useState('');
  const [message, setMessage] = useState('');
  const [sending, setSending] = useState(false);
  const [results, setResults] = useState([]);
  const [sqlQuery, setSqlQuery] = useState('');
  const [generating, setGenerating] = useState(false);

  const addRecipient = () => {
    const phone = phoneInput.trim();
    if (!phone) return toast.error('Enter a phone number');
    if (!/^\+?\d{7,15}$/.test(phone.replace(/[\s\-()]/g, '')))
      return toast.error('Invalid phone number format (include country code, e.g. +91XXXXXXXXXX)');
    if (recipients.includes(phone)) return toast.error('Already added');
    setRecipients([...recipients, phone]);
    setPhoneInput('');
  };

  const removeRecipient = (phone) => setRecipients(recipients.filter(r => r !== phone));

  const generateMessage = async () => {
    if (!sqlQuery.trim()) return toast.error('Enter a SQL query first');
    setGenerating(true);
    try {
      // First fetch query data
      const queryRes = await api.post('/api/database/query', { sql: sqlQuery, page: 0, pageSize: 20 });
      const { columns, rows } = queryRes.data.data;

      // Then generate message
      const msgRes = await api.post('/api/whatsapp/generate-message', {
        columns, rows, header: 'Database Report'
      });
      setMessage(msgRes.data.data);
      toast.success('Message generated from query data');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to generate message');
    } finally {
      setGenerating(false);
    }
  };

  const sendMessages = async () => {
    if (recipients.length === 0) return toast.error('Add at least one recipient');
    if (!message.trim()) return toast.error('Message cannot be empty');
    setSending(true);
    try {
      const res = await api.post('/api/whatsapp/send', { recipients, message });
      setResults(res.data.data || []);
      toast.success('Messages processed!');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Send failed');
    } finally {
      setSending(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <Title level={4} style={{ margin: 0 }}>
          <WhatsAppOutlined style={{ color: '#25D366', marginRight: 8 }} />
          WhatsApp Messaging
        </Title>
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={14}>
          <Card style={{ borderRadius: 12, marginBottom: 16 }} title="📱 Recipients">
            <Space.Compact style={{ width: '100%', marginBottom: 12 }}>
              <Input
                value={phoneInput}
                onChange={e => setPhoneInput(e.target.value)}
                placeholder="+91XXXXXXXXXX (with country code)"
                prefix={<UserOutlined />}
                onPressEnter={addRecipient}
              />
              <Button icon={<PlusOutlined />} onClick={addRecipient} type="primary"
                style={{ background: '#25D366', borderColor: '#25D366' }}>
                Add
              </Button>
            </Space.Compact>

            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
              {recipients.map(phone => (
                <Tag key={phone} closable onClose={() => removeRecipient(phone)}
                  color="green" style={{ padding: '4px 8px', fontSize: 13 }}>
                  {phone}
                </Tag>
              ))}
              {recipients.length === 0 && <Text type="secondary">No recipients added</Text>}
            </div>
          </Card>

          <Card style={{ borderRadius: 12, marginBottom: 16 }} title="🤖 Generate from Query">
            <TextArea
              value={sqlQuery}
              onChange={e => setSqlQuery(e.target.value)}
              placeholder="SELECT * FROM employee_salary WHERE month = 'January'"
              rows={3}
              style={{ fontFamily: 'monospace', fontSize: 13, marginBottom: 10 }}
            />
            <Button onClick={generateMessage} loading={generating} icon={<MessageOutlined />}>
              Generate Message from Query
            </Button>
          </Card>

          <Card style={{ borderRadius: 12 }} title="✍️ Message">
            <TextArea
              value={message}
              onChange={e => setMessage(e.target.value)}
              placeholder="Type your WhatsApp message here... Supports *bold*, _italic_"
              rows={8}
              style={{ marginBottom: 12 }}
              showCount
              maxLength={4096}
            />
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Text type="secondary" style={{ fontSize: 12 }}>
                Sending to {recipients.length} recipient(s)
              </Text>
              <Button
                type="primary"
                icon={<SendOutlined />}
                onClick={sendMessages}
                loading={sending}
                disabled={recipients.length === 0 || !message.trim()}
                size="large"
                style={{ background: '#25D366', borderColor: '#25D366' }}
              >
                Send via WhatsApp
              </Button>
            </div>
          </Card>
        </Col>

        <Col xs={24} lg={10}>
          {/* Delivery Status */}
          <Card style={{ borderRadius: 12 }} title="📊 Delivery Status">
            {results.length === 0 ? (
              <Text type="secondary">Send messages to see delivery status.</Text>
            ) : (
              <List
                dataSource={results}
                renderItem={r => (
                  <List.Item>
                    <List.Item.Meta
                      title={<Text>{r.recipient}</Text>}
                      description={r.error || r.messageId || ''}
                    />
                    <Badge
                      status={r.status === 'SENT' ? 'success' : 'error'}
                      text={r.status}
                    />
                  </List.Item>
                )}
              />
            )}
          </Card>

          <Card style={{ borderRadius: 12, marginTop: 16 }} title="ℹ️ Tips">
            <Space direction="vertical" style={{ width: '100%' }}>
              <Alert message="Use country code without + for some carriers (e.g. 919XXXXXXXX)" type="info" showIcon style={{ borderRadius: 8 }} />
              <Alert message="WhatsApp messages support *bold*, _italic_, ~strikethrough~" type="info" showIcon style={{ borderRadius: 8 }} />
              <Alert message="Max 20 rows shown in generated messages" type="warning" showIcon style={{ borderRadius: 8 }} />
            </Space>
          </Card>
        </Col>
      </Row>
    </div>
  );
}