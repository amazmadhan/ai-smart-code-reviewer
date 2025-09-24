import React, {useState} from 'react'
import axios from 'axios'
import { saveAs } from 'file-saver'

export default function UploadForm(){
  const [file, setFile] = useState(null)
  const [analysis, setAnalysis] = useState(null)
  const [loading, setLoading] = useState(false)
  const [refactoring, setRefactoring] = useState(false)

  const onFileChange = e => setFile(e.target.files[0])

  const analyze = async () => {
    if (!file) return alert('Select a .java file first')
    setLoading(true)
    const form = new FormData(); form.append('file', file)
    try{
      const res = await axios.post('http://localhost:8080/api/analyze', form, { headers: {'Content-Type':'multipart/form-data'} })
      setAnalysis(res.data)
    }catch(err){ alert('Analyze failed: '+err) } finally { setLoading(false) }
  }

  const downloadOriginal = () => {
    if (!analysis) return
    const blob = new Blob([analysis.originalSource], {type: 'text/plain;charset=utf-8'})
    saveAs(blob, analysis.fileName || 'original.java')
  }
  const downloadRefactored = () => {
    if (!analysis || !analysis.refactoredSource) return
    const blob = new Blob([analysis.refactoredSource], {type: 'text/plain;charset=utf-8'})
    saveAs(blob, (analysis.fileName? analysis.fileName.replace('.java','') : 'refactored') + '_refactored.java')
  }

  return (<div className="container">
    <div className="file-input-container">
      <input type='file' accept='.java' onChange={onFileChange} />
      <button className="action-button" onClick={analyze}>Review & Refactor (AI)</button>
    </div>
    {loading && <p>Analyzing...</p>}
    {refactoring && <p>Refactoring using AI...</p>}
    {analysis && (<div className='result'>
      <div className="file-info">
        <div className="file-name">File: {analysis.fileName}</div>
        <div className="scores-container">
          <div className="score-wrapper">
            <div className="score-label">Original score:</div>
            <div className="score-display original-score">{analysis.originalScore}</div>
          </div>
          <div className="score-wrapper">
            <div className="score-label">AI score:</div>
            <div className="score-display refactored-score">{analysis.refactoredScore}</div>
          </div>
        </div>
        <div className="issues-section">
          <div className="issues-title">Issues (line : description)</div>
          <div className="issues-list">
            <ul>{analysis.issues && analysis.issues.map((it, idx)=>(<li key={idx}>Line {it.line} : {it.message}</li>))}</ul>
          </div>
        </div>
      </div>

      <h4>AI Suggestions</h4>
      <pre className='code'>{analysis.aiSuggestions}</pre>
      <h4>Refactored Source</h4>
      <pre className='code'>{analysis.refactoredSource}</pre>

      <div className="buttons-container">
        <button className="download-btn" onClick={downloadOriginal}>Download Original Code</button>
        <button className="download-btn" onClick={downloadRefactored}>Download Refactored Code</button>
      </div>
    </div>)}
  </div>)
}
