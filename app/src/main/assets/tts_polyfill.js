/* Kaala Kolam: Android TextToSpeech fallback.
   Injected at document start only for the appassets origin. When the native bridge is present,
   speech is always routed through Android TextToSpeech so late-loading WebView voices cannot
   leave the page with incompatible SpeechSynthesisVoice objects. */
(function () {
  'use strict';
  if (window.__kkTtsFallback || typeof AndroidTTS === 'undefined') return;
  window.__kkTtsFallback = true;

  var pending = {};
  var sequence = 0;
  var speaking = false;
  var voices = [
    { name: 'Android voice (English, India)', lang: 'en-IN', localService: true, default: true, voiceURI: 'android-en-IN' },
    { name: 'Android voice (Tamil, India)', lang: 'ta-IN', localService: true, default: false, voiceURI: 'android-ta-IN' }
  ];

  function post(message) {
    try { AndroidTTS.postMessage(JSON.stringify(message)); } catch (e) { /* bridge unavailable */ }
  }

  function Utterance(text) {
    this.text = String(text == null ? '' : text);
    this.lang = '';
    this.voice = null;
    this.rate = 1;
    this.pitch = 1;
    this.volume = 1;
    this.onstart = null;
    this.onend = null;
    this.onerror = null;
  }

  AndroidTTS.onmessage = function (event) {
    var message;
    try { message = JSON.parse(event.data); } catch (e) { return; }
    var utterance = pending[message.id];
    if (!utterance) return;
    if (message.ev === 'start') {
      speaking = true;
      if (utterance.onstart) utterance.onstart({ type: 'start' });
      return;
    }
    speaking = false;
    delete pending[message.id];
    if (message.ev === 'end') {
      if (utterance.onend) utterance.onend({ type: 'end' });
    } else if (utterance.onerror) {
      utterance.onerror({ type: 'error', error: message.reason || 'synthesis-failed' });
    }
  };

  var synth = {
    getVoices: function () { return voices.slice(); },
    speak: function (utterance) {
      if (!utterance) return;
      var id = 'kk' + (++sequence);
      pending[id] = utterance;
      post({
        op: 'speak',
        id: id,
        text: String(utterance.text || ''),
        lang: (utterance.voice && utterance.voice.lang) || utterance.lang || 'en-IN',
        rate: Number(utterance.rate) || 1
      });
    },
    cancel: function () {
      pending = {};
      speaking = false;
      post({ op: 'stop' });
    },
    pause: function () {},
    resume: function () {},
    addEventListener: function () {},
    removeEventListener: function () {}
  };
  Object.defineProperty(synth, 'speaking', { get: function () { return speaking; } });

  try {
    Object.defineProperty(window, 'speechSynthesis', { value: synth, configurable: true, writable: true });
  } catch (e) {
    try { window.speechSynthesis = synth; } catch (ignored) {}
  }

  /* Always pair the bridge voices with the bridge utterance type. A native WebView constructor
     can reject the plain bridge voice object assigned by the page, which was the original bug. */
  try {
    Object.defineProperty(window, 'SpeechSynthesisUtterance',
      { value: Utterance, configurable: true, writable: true });
  } catch (e) {
    window.SpeechSynthesisUtterance = Utterance;
  }
})();
